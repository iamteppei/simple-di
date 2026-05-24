package io.abc.shared.di.core;

import io.abc.shared.di.BeanCreationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactoryTypedTest {

    @Test
    void singleton_create_withWrongType_throwsException() {
        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                null,
                null,
                ServiceImpl::new,
                null,
                null,
                null
        );

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> singleton.create(String.class));

        assertTrue(error.getMessage().contains("is invalid"));
    }

    @Test
    void singleton_onContextInitialize_wrapsFactoryFailure() {
        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                null,
                null,
                () -> {
                    throw new IllegalArgumentException("boom");
                },
                null,
                null,
                null
        );

        final BeanCreationException error = assertThrows(BeanCreationException.class, singleton::onContextInitialize);

        assertTrue(error.getMessage().contains("Fail creation of bean"));
    }

    @Test
    void singleton_lifecycleCallbacks_areInvokedOnce() {
        final AtomicInteger hookCalls = new AtomicInteger(0);
        final AtomicInteger postConstructCalls = new AtomicInteger(0);
        final AtomicInteger preDestroyCalls = new AtomicInteger(0);
        final AtomicInteger readyCalls = new AtomicInteger(0);

        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                impl -> postConstructCalls.incrementAndGet(),
                impl -> preDestroyCalls.incrementAndGet(),
                ServiceImpl::new,
                List.of(impl -> hookCalls.incrementAndGet()),
                List.of(impl -> hookCalls.incrementAndGet()),
                impl -> readyCalls.incrementAndGet()
        );

        singleton.onContextInitialize();
        singleton.onInitializeHooks();
        singleton.onInitializeHooks();
        singleton.onPostContextInitialize();
        singleton.onPostContextInitialize();
        singleton.onContextReady();
        singleton.onContextReady();
        singleton.onContextDestroy();
        singleton.onContextDestroy();

        assertEquals(2, hookCalls.get());
        assertEquals(1, postConstructCalls.get());
        assertEquals(1, preDestroyCalls.get());
        assertEquals(1, readyCalls.get());
    }

    @Test
    void singleton_create_returnsSameInstance() {
        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                null,
                null,
                ServiceImpl::new,
                null,
                null,
                null
        );

        final Service first = singleton.create(Service.class);
        final Service second = singleton.create(Service.class);

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void singleton_create_concurrentCalls_coverCompareAndSetFalseBranch() throws InterruptedException {
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicInteger createdCount = new AtomicInteger(0);
        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                null,
                null,
                () -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException exp) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exp);
                    }
                    createdCount.incrementAndGet();
                    return new ServiceImpl();
                },
                null,
                null,
                null
        );

        final AtomicReference<Service> firstRef = new AtomicReference<>();
        final AtomicReference<Service> secondRef = new AtomicReference<>();

        final Thread t1 = new Thread(() -> firstRef.set(singleton.create(Service.class)));
        final Thread t2 = new Thread(() -> secondRef.set(singleton.create(Service.class)));

        t1.start();
        t2.start();

        ready.await();
        go.countDown();

        t1.join();
        t2.join();

        assertNotNull(firstRef.get());
        assertSame(firstRef.get(), secondRef.get());
        assertEquals(2, createdCount.get());
    }

    @Test
    void singleton_onContextReady_wrapsCallbackFailure() {
        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                null,
                null,
                ServiceImpl::new,
                null,
                null,
                impl -> {
                    throw new IllegalStateException("ready boom");
                }
        );

        singleton.onContextInitialize();

        final BeanCreationException error = assertThrows(BeanCreationException.class, singleton::onContextReady);

        assertTrue(error.getMessage().contains("Fail onContextReady"));
    }

    @Test
    void singleton_lifecycleMethods_beforeInitialization_areNoop() {
        final FactoryTypedSingleton<Service, ServiceImpl> singleton = new FactoryTypedSingleton<>(
                Service.class,
                ServiceImpl.class,
                null,
                null,
                ServiceImpl::new,
                List.of(impl -> { }),
                List.of(impl -> { }),
                impl -> { }
        );

        singleton.onContextDestroy();
        singleton.onPostContextInitialize();
        singleton.onContextReady();
    }

    @Test
    void prototype_create_withWrongType_throwsException() {
        final FactoryTypedPrototype<Service, ServiceImpl> prototype = new FactoryTypedPrototype<>(
                Service.class,
                ServiceImpl.class,
                ServiceImpl::new,
                null,
                null
        );

        final BeanCreationException error = assertThrows(BeanCreationException.class, () -> prototype.create(String.class));

        assertTrue(error.getMessage().contains("is invalid"));
    }

    @Test
    void prototype_getImplBeanName_returnsImplementationName() {
        final FactoryTypedPrototype<Service, ServiceImpl> prototype = new FactoryTypedPrototype<>(
                Service.class,
                ServiceImpl.class,
                ServiceImpl::new,
                null,
                null
        );

        assertEquals(ServiceImpl.class.getName(), prototype.getImplBeanName());
    }

    @Test
    void prototype_create_wrapsFactoryFailure() {
        final FactoryTypedPrototype<Service, ServiceImpl> prototype = new FactoryTypedPrototype<>(
                Service.class,
                ServiceImpl.class,
                () -> {
                    throw new IllegalArgumentException("factory boom");
                },
                null,
                null
        );

        final BeanCreationException error = assertThrows(BeanCreationException.class, () -> prototype.create(Service.class));

        assertTrue(error.getMessage().contains("Fail creation of bean"));
    }

    @Test
    void prototype_create_runsHooksBeforePostConstruct() {
        final List<String> sequence = new ArrayList<>();

        final FactoryTypedPrototype<Service, ServiceImpl> prototype = new FactoryTypedPrototype<>(
                Service.class,
                ServiceImpl.class,
                ServiceImpl::new,
                impl -> sequence.add("postConstruct"),
                List.of(impl -> sequence.add("availableHook"))
        );

        final Service created = prototype.create(Service.class);

        assertNotNull(created);
        assertEquals(List.of("availableHook", "postConstruct"), sequence);
    }

    @Test
    void prototype_create_withNullHooksAndNullPostConstruct_succeeds() {
        final FactoryTypedPrototype<Service, ServiceImpl> prototype = new FactoryTypedPrototype<>(
                Service.class,
                ServiceImpl.class,
                ServiceImpl::new,
                null,
                null
        );

        final Service created = prototype.create(Service.class);

        assertNotNull(created);
    }

    @Test
    void prototype_lifecycleNoopMethods_doNotThrow() {
        final FactoryTypedPrototype<Service, ServiceImpl> prototype = new FactoryTypedPrototype<>(
                Service.class,
                ServiceImpl.class,
                ServiceImpl::new,
                null,
                null
        );

        prototype.onContextInitialize();
        prototype.onInitializeHooks();
        prototype.onPostContextInitialize();
        prototype.onContextReady();
        prototype.onContextDestroy();
    }

    interface Service {
    }

    static final class ServiceImpl implements Service {
    }
}
