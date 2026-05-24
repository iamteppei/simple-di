package io.abc.shared.di.core;

import io.abc.shared.di.AppContext;
import io.abc.shared.di.Module;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerImplTest {

    @Test
    void up_withModules_startsAndExposesContext() {
        final ContainerImpl container = new ContainerImpl();

        final AppContext context = container.up(List.of(new HealthyModule()), new String[0]);

        assertNotNull(context);
        assertSame(context, container.getContext());
    }

    @Test
    void up_whenAlreadyRunning_returnsExistingContextWithoutReconfiguring() {
        final ContainerImpl container = new ContainerImpl();

        final AppContext first = container.up(List.of(new HealthyModule()), new String[0]);
        final AppContext second = container.up(List.of(new FailingModule()), new String[0]);

        assertSame(first, second);
    }

    @Test
    void down_isIdempotent_andClearsContext() {
        final ContainerImpl container = new ContainerImpl();

        assertDoesNotThrow(container::down);
        assertNull(container.getContext());

        container.up(List.of(new HealthyModule()), new String[0]);
        container.down();

        assertNull(container.getContext());
        assertDoesNotThrow(container::down);
    }

    @Test
    void up_whenConfigurationFails_allowsRestartFromFailedState() {
        final ContainerImpl container = new ContainerImpl();

        assertThrows(IllegalStateException.class, () -> container.up(List.of(new FailingModule()), new String[0]));
        assertNull(container.getContext());

        final AppContext recovered = container.up(List.of(new HealthyModule()), new String[0]);

        assertNotNull(recovered);
        assertSame(recovered, container.getContext());
    }

    @Test
    void down_invokesPreDestroyOnce() {
        final ContainerImpl container = new ContainerImpl();
        final AtomicInteger preDestroyCalls = new AtomicInteger(0);

        container.up(List.of(new LifecycleModule(preDestroyCalls)), new String[0]);
        container.down();
        container.down();

        assertEquals(1, preDestroyCalls.get());
    }

    @Test
    void up_whenStateStarting_throwsTransitionException() {
        final ContainerImpl container = new ContainerImpl();
        setState(container, "STARTING");

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> container.up(List.of(new HealthyModule()), new String[0]));

        assertEquals("Container is in transition state: STARTING", error.getMessage());
    }

    @Test
    void up_whenStateStopping_throwsTransitionException() {
        final ContainerImpl container = new ContainerImpl();
        setState(container, "STOPPING");

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> container.up(List.of(new HealthyModule()), new String[0]));

        assertEquals("Container is in transition state: STOPPING", error.getMessage());
    }

    @Test
    void down_whenStateFailedWithNoContext_isNoop() {
        final ContainerImpl container = new ContainerImpl();
        setState(container, "FAILED");

        assertDoesNotThrow(container::down);
        assertNull(container.getContext());
    }

    @Test
    void down_whenDestroyFails_propagatesException() {
        final ContainerImpl container = new ContainerImpl();

        container.up(List.of(new FailingDestroyModule()), new String[0]);

        final IllegalStateException error = assertThrows(IllegalStateException.class, container::down);

        assertEquals("destroy failed", error.getMessage());
        assertNull(container.getContext());
    }

    @Test
    void up_whenStateIsNull_throwsInvalidStateException() {
        final ContainerImpl container = new ContainerImpl();
        setRawState(container, null);

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> container.up(List.of(new HealthyModule()), new String[0]));

        assertEquals("Container can not start from state: null", error.getMessage());
    }

    @Test
    void lifecycleHooks_coverAllBindingUseMethods() {
        final ContainerImpl container = new ContainerImpl();
        final AtomicInteger postConstructCalls = new AtomicInteger(0);
        final AtomicInteger preDestroyCalls = new AtomicInteger(0);
        final AtomicInteger postContainerInitializedCalls = new AtomicInteger(0);
        final AtomicInteger availableHookCalls = new AtomicInteger(0);
        final AtomicInteger destroyHookCalls = new AtomicInteger(0);

        container.up(List.of(new FullLifecycleModule(
                postConstructCalls,
                preDestroyCalls,
                postContainerInitializedCalls,
                availableHookCalls,
                destroyHookCalls
        )), new String[0]);

        assertEquals(1, postConstructCalls.get());
        assertEquals(1, postContainerInitializedCalls.get());
        assertEquals(1, availableHookCalls.get());

        container.down();

        assertEquals(1, preDestroyCalls.get());
        assertEquals(0, destroyHookCalls.get());
    }

    interface HealthService {
        String status();
    }

    static final class HealthServiceImpl implements HealthService {
        @Override
        public String status() {
            return "UP";
        }
    }

    interface LifecycleService {
        String name();
    }

    interface DependencyService {
        String id();
    }

    static final class LifecycleServiceImpl implements LifecycleService {
        @Override
        public String name() {
            return "lifecycle";
        }
    }

    static final class DependencyServiceImpl implements DependencyService {
        @Override
        public String id() {
            return "dep";
        }
    }

    static final class HealthyModule extends Module {
        @Override
        public void configure() {
            bind(HealthService.class, HealthServiceImpl.class, HealthServiceImpl::new);
        }
    }

    static final class FailingModule extends Module {
        @Override
        public void configure() {
            throw new IllegalStateException("configure failed");
        }
    }

    static final class LifecycleModule extends Module {

        private final AtomicInteger preDestroyCalls;

        private LifecycleModule(AtomicInteger preDestroyCalls) {
            this.preDestroyCalls = preDestroyCalls;
        }

        @Override
        public void configure() {
            bind(LifecycleService.class, LifecycleServiceImpl.class, binding ->
                    binding
                            .factory(LifecycleServiceImpl::new)
                            .usePreDestroy(impl -> preDestroyCalls.incrementAndGet())
            );
        }
    }

    static final class FullLifecycleModule extends Module {

        private final AtomicInteger postConstructCalls;
        private final AtomicInteger preDestroyCalls;
        private final AtomicInteger postContainerInitializedCalls;
        private final AtomicInteger availableHookCalls;
        private final AtomicInteger destroyHookCalls;

        private FullLifecycleModule(
                AtomicInteger postConstructCalls,
                AtomicInteger preDestroyCalls,
                AtomicInteger postContainerInitializedCalls,
                AtomicInteger availableHookCalls,
                AtomicInteger destroyHookCalls
        ) {
            this.postConstructCalls = postConstructCalls;
            this.preDestroyCalls = preDestroyCalls;
            this.postContainerInitializedCalls = postContainerInitializedCalls;
            this.availableHookCalls = availableHookCalls;
            this.destroyHookCalls = destroyHookCalls;
        }

        @Override
        public void configure() {
            bind(DependencyService.class, DependencyServiceImpl.class, DependencyServiceImpl::new);

            bind(LifecycleService.class, LifecycleServiceImpl.class, binding ->
                    binding
                            .factory(LifecycleServiceImpl::new)
                            .usePostConstruct(impl -> postConstructCalls.incrementAndGet())
                            .usePreDestroy(impl -> preDestroyCalls.incrementAndGet())
                            .usePostContainerInitialized(impl -> postContainerInitializedCalls.incrementAndGet())
                            .useBeanHook(
                                    DependencyService.class,
                                    (impl, dep) -> availableHookCalls.incrementAndGet(),
                                    (impl, dep) -> destroyHookCalls.incrementAndGet()
                            )
            );
        }
    }

    static final class FailingDestroyModule extends Module {
        @Override
        public void configure() {
            bind(LifecycleService.class, LifecycleServiceImpl.class, binding ->
                    binding
                            .factory(LifecycleServiceImpl::new)
                            .usePreDestroy(impl -> {
                                throw new IllegalStateException("destroy failed");
                            })
            );
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setState(ContainerImpl container, String stateName) {
        try {
            final Field stateField = ContainerImpl.class.getDeclaredField("state");
            stateField.setAccessible(true);
            final Class<?> stateType = stateField.getType();
            final Enum<?> state = Enum.valueOf((Class<Enum>) stateType, stateName);
            stateField.set(container, state);
        } catch (ReflectiveOperationException exp) {
            throw new AssertionError("Unable to set container state", exp);
        }
    }

    private static void setRawState(ContainerImpl container, Object state) {
        try {
            final Field stateField = ContainerImpl.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(container, state);
        } catch (ReflectiveOperationException exp) {
            throw new AssertionError("Unable to set raw container state", exp);
        }
    }
}
