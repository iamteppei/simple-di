package io.abc.shared.di.core;

import io.abc.shared.di.Binding;
import io.abc.shared.di.Scope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingImplTest {

    @Test
    void factory_withNullSupplier_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> binding.factory(null));

        assertTrue(error.getMessage().contains("does not accept a null factory"));
    }

    @Test
    void factory_whenSetTwice_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        binding.factory(ServiceImpl::new);

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> binding.factory(ServiceImpl::new));

        assertTrue(error.getMessage().contains("Factory is already set"));
    }

    @Test
    void usePostConstruct_withNull_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> binding.usePostConstruct(null));

        assertTrue(error.getMessage().contains("PostConstruct must not be null"));
    }

    @Test
    void usePreDestroy_withNull_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> binding.usePreDestroy(null));

        assertTrue(error.getMessage().contains("PreDestroy must not be null"));
    }

    @Test
    void usePostContainerInitialized_withNull_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> binding.usePostContainerInitialized(null));

        assertTrue(error.getMessage().contains("must not be null"));
    }

    @Test
    void scope_withNull_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> binding.scope(null));

        assertTrue(error.getMessage().contains("Scope must not be null"));
    }

    @Test
    void useBeanHook_withClassType_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> binding.useBeanHook(ServiceImpl.class, (impl, dep) -> { }, (impl, dep) -> { }));

        assertTrue(error.getMessage().contains("must be an interface"));
    }

    @Test
    void create_withoutFactory_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        final IllegalStateException error = assertThrows(IllegalStateException.class, binding::create);

        assertTrue(error.getMessage().contains("missed to defined a factory method"));
    }

    @Test
    void create_withUnsupportedScope_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        binding.factory(ServiceImpl::new).scope(Scope.PLUGIN);

        final IllegalStateException error = assertThrows(IllegalStateException.class, binding::create);

        assertTrue(error.getMessage().contains("is not supported"));
    }

    @Test
    void create_withNullInternalScope_throwsException() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());
        binding.factory(ServiceImpl::new);
        setScope(binding, null);

        final IllegalStateException error = assertThrows(IllegalStateException.class, binding::create);

        assertTrue(error.getMessage().contains("null is not supported"));
    }

    @Test
    void create_withPrototypeScope_returnsPrototypeFactory() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        binding.factory(ServiceImpl::new)
                .useBeanHook(Dependency.class, (impl, dep) -> { }, null)
                .scope(Scope.PROTOTYPE);

        final Factory factory = binding.create();

        assertTrue(factory instanceof FactoryTypedPrototype);
    }

    @Test
    void create_withSingletonScope_returnsSingletonFactory() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        binding.factory(ServiceImpl::new)
                .scope(Scope.SINGLETON)
                .useBeanHook(Dependency.class, null, (impl, dep) -> { })
                .usePostConstruct(impl -> { })
                .usePreDestroy(impl -> { })
                .usePostContainerInitialized(impl -> { });

        final Factory factory = binding.create();

        assertTrue(factory instanceof FactoryTypedSingleton);
    }

    @Test
    void useBeanHook_invokesAvailableAndDestroyCallbacks() {
        final AtomicInteger availableCalls = new AtomicInteger(0);
        final AtomicInteger destroyCalls = new AtomicInteger(0);
        final Dependency dependency = new Dependency() { };
        final StubListableContext context = new StubListableContext(List.of(dependency));
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, context);

        binding.factory(ServiceImpl::new)
                .useBeanHook(
                        Dependency.class,
                        (impl, dep) -> availableCalls.incrementAndGet(),
                        (impl, dep) -> destroyCalls.incrementAndGet()
                );

        final Factory factory = binding.create();
        factory.onContextInitialize();
        factory.onInitializeHooks();
        factory.onInitializeHooks();
        factory.onContextDestroy();
        factory.onContextDestroy();

        assertEquals(1, availableCalls.get());
        assertEquals(1, destroyCalls.get());
    }

    @Test
    void useBeanHook_calledTwice_reusesHookLists() {
        final BindingImpl<Service, ServiceImpl> binding = new BindingImpl<>(Service.class, ServiceImpl.class, new ListableContextImpl());

        binding.factory(ServiceImpl::new)
                .useBeanHook(Dependency.class, (impl, dep) -> { }, (impl, dep) -> { })
                .useBeanHook(Dependency.class, (impl, dep) -> { }, (impl, dep) -> { });

        final Factory factory = binding.create();

        assertTrue(factory instanceof FactoryTypedSingleton);
    }

    interface Service {
    }

    interface Dependency {
    }

    static final class ServiceImpl implements Service {
    }

    static final class StubListableContext implements ListableContext {

        private final Collection<?> dependencies;

        StubListableContext(Collection<?> dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public <T> T get(String id, Class<T> type) {
            throw new UnsupportedOperationException("Not required by this test");
        }

        @Override
        public <T, I extends T> void bind(Class<T> type, Class<I> impl, String id, Consumer<Binding<T, I>> binding) {
            throw new UnsupportedOperationException("Not required by this test");
        }

        @Override
        public <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl, String id) {
            throw new UnsupportedOperationException("Not required by this test");
        }

        @Override
        public <T> void bind(Class<T> type, String id, Consumer<Binding<T, T>> binding) {
            throw new UnsupportedOperationException("Not required by this test");
        }

        @Override
        public <T> Binding<T, T> bind(Class<T> type, String id) {
            throw new UnsupportedOperationException("Not required by this test");
        }

        @Override
        public <T> Collection<T> listAll(Class<T> type) {
            return dependencies.stream().map(type::cast).toList();
        }
    }

    private static void setScope(BindingImpl<Service, ServiceImpl> binding, Scope scope) {
        try {
            final Field field = BindingImpl.class.getDeclaredField("scope");
            field.setAccessible(true);
            field.set(binding, scope);
        } catch (ReflectiveOperationException exp) {
            throw new AssertionError("Unable to set scope", exp);
        }
    }
}
