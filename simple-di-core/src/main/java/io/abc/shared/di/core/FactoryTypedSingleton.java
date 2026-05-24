package io.abc.shared.di.core;

import io.abc.shared.di.BeanCreationException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FactoryTypedSingleton<T, IMPL extends T> implements FactoryTyped<T, IMPL> {

    private final Class<T> type;
    private final Class<IMPL> implType;
    private final Supplier<IMPL> factory;
    private final Consumer<IMPL> onContextInitialized;
    private final Consumer<IMPL> preDestroy;
    private final Consumer<IMPL> postConstruct;
    private final List<HookFunc<IMPL>> availableHooks;
    private final List<HookFunc<IMPL>> destroyHooks;
    private final AtomicReference<IMPL> instanceRef = new AtomicReference<>();
    private final AtomicBoolean hookInitialized = new AtomicBoolean();
    private final AtomicBoolean destroyHookInitialized = new AtomicBoolean();
    private final AtomicBoolean postConstructInitialized = new AtomicBoolean();
    private final AtomicBoolean postContainerInitialized = new AtomicBoolean();

    public FactoryTypedSingleton(Class<T> type, Class<IMPL> implType, Consumer<IMPL> postConstruct, Consumer<IMPL> preDestroy, Supplier<IMPL> factory, List<HookFunc<IMPL>> availableHooks, List<HookFunc<IMPL>> destroyHooks, Consumer<IMPL> onContextInitialized) {
        this.type = type;
        this.implType = implType;
        this.postConstruct = postConstruct;
        this.preDestroy = preDestroy;
        this.factory = factory;
        this.availableHooks = availableHooks;
        this.destroyHooks = destroyHooks;
        this.onContextInitialized = onContextInitialized;
    }

    @Override
    public String getImplBeanName() {
        return implType.getName();
    }

    @Override
    public <B> B create(Class<B> type) {
        if (this.type.equals(type)) {
            return type.cast(getOrCreate());
        }
        throw new IllegalStateException(type.getName() + " is invalid. Expected " + this.type.getName());
    }

    private IMPL getOrCreate() {
        final IMPL current = instanceRef.get();
        if (current != null) {
            return current;
        }
        // note that, the bean is not ready yet at this stage as beanHooks and postConstruct are not completed yet.
        // calling beanHooks at this stage will also cause issues because the hooked bean may not be fully initialized yet,
        // so we only set the instance here and call the hooks at the end of onPostContextInitialize.
        try {
            final IMPL created = factory.get();
            if (instanceRef.compareAndSet(null, created)) {
                return created;
            }
            return instanceRef.get();
        } catch (Exception exp) {
            throw new BeanCreationException("Fail creation of bean " + implType.getName() + " with singleton scope", exp);
        }
    }

    @Override
    public void onInitializeHooks() {
        if (hookInitialized.compareAndSet(false, true)) {
            if (availableHooks != null) {
                final IMPL current = getOrCreate();
                for (final HookFunc<IMPL> availableHook : availableHooks) {
                    availableHook.apply(current);
                }
            }
        }
    }

    @Override
    public void onContextDestroy() {
        final IMPL current = instanceRef.get();
        if (current == null) {
            return;
        }
        if (destroyHookInitialized.compareAndSet(false, true)) {
            if (destroyHooks != null) {
                for (final HookFunc<IMPL> destroyHook : destroyHooks) {
                    destroyHook.apply(current);
                }
            }
            if (preDestroy != null) {
                preDestroy.accept(current);
            }
        }
    }

    @Override
    public void onPostContextInitialize() {
        final IMPL current = instanceRef.get();
        if (current == null) {
            return;
        }
        if (postConstructInitialized.compareAndSet(false, true)) {
            if (postConstruct != null) {
                postConstruct.accept(current);
            }
        }
    }

    @Override
    public void onContextReady() {
        final IMPL current = instanceRef.get();
        if (current == null) {
            return;
        }
        if (postContainerInitialized.compareAndSet(false, true)) {
            if (this.onContextInitialized != null) {
                try {
                    onContextInitialized.accept(current);
                } catch (Exception exp) {
                    // TODO at this point, if the onRefresh fails, throwing a BeanCreationException will kill the running application. Review if this is the desired behavior
                    throw new BeanCreationException("Fail onContextReady of bean " + type.getName() + " with singleton scope", exp);
                }
            }
        }
    }

    @Override
    public void onContextInitialize() {
        // don't support lazy initialization, so call the method to ensure the instance is created
        getOrCreate();
    }
}
