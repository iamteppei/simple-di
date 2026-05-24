package io.abc.shared.di.core;

import io.abc.shared.di.AppContext;
import io.abc.shared.di.ConfigurableContext;
import io.abc.shared.di.ConfigurableModule;
import io.abc.shared.di.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class ContainerImpl implements Container {

    private static final Logger logger = LoggerFactory.getLogger(ContainerImpl.class);
    private final ReentrantLock lock = new ReentrantLock();
    private ListableContextImpl context;
    private Thread shutdownHook;
    private State state = State.NEW;

    @Override
    public AppContext up(List<ConfigurableModule> modules, String[] args) {
        lock.lock();
        try {
            if (state == State.RUNNING) {
                return context;
            }

            if (state == State.STARTING || state == State.STOPPING) {
                throw new IllegalStateException("Container is in transition state: " + state);
            }

            if (state != State.NEW && state != State.STOPPED && state != State.FAILED) {
                throw new IllegalStateException("Container can not start from state: " + state);
            }

            state = State.STARTING;
            final Instant start = Instant.now();
            final ListableContextImpl contextImpl = new ListableContextImpl();

            try {
                for (final ConfigurableModule module : modules) {
                    module.setContext(contextImpl);
                    module.configure();
                    logger.debug(">> Configured [{}]", module.getClass().getName());
                }

                // phase 1: create instance of beans and resolve dependencies
                contextImpl.initialize();

                // phase 2: init useBeenHooks
                contextImpl.initializeHooks();

                // phase 3: init postConstruct
                contextImpl.postInitialize();

                // phase 4: init postContainerInitialized
                contextImpl.onReady();

                final Thread hook = new Thread(this::down, "container-pure-shutdown-" + System.identityHashCode(this));
                Runtime.getRuntime().addShutdownHook(hook);

                this.context = contextImpl;
                this.shutdownHook = hook;
                this.state = State.RUNNING;
                logger.info(">> Started container in [{}] mils", Duration.between(start, Instant.now()).toMillis());
                return context;
            } catch (Exception exp) {
                this.state = State.FAILED;
                contextImpl.down();
                throw exp;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void down() {
        lock.lock();
        try {
            if (state == State.NEW || state == State.STOPPED || state == State.STOPPING) {
                return;
            }

            state = State.STOPPING;
            RuntimeException downError = null;

            if (this.context != null) {
                final ListableContextImpl ctx = context;
                context = null;
                try {
                    ctx.down();
                } catch (RuntimeException exp) {
                    downError = exp;
                }
            }

            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException exp) {
                    // JVM is already shutting down.
                } finally {
                    shutdownHook = null;
                }
            }

            state = State.STOPPED;
            if (downError != null) {
                throw downError;
            }
        } finally {
            lock.unlock();
        }
    }

    ConfigurableContext getContext() {
        return context;
    }

    private enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        FAILED
    }
}
