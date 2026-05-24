package io.abc.shared.di.core;

import io.abc.shared.di.BeanCreationException;
import io.abc.shared.di.Module;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListableContextImplTest {

    @Test
    void get_withDefaultIdAndSingleBean_resolvesBean() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(Service.class, ServiceImpl.class, null).factory(ServiceImpl::new);
        context.initialize();

        final Service service = context.get(Module.DEFAULT_ID, Service.class);

        assertEquals("ok", service.value());
    }

    @Test
    void get_withUnknownType_throwsException() {
        final ListableContextImpl context = new ListableContextImpl();

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.get("missing", Service.class));

        assertTrue(error.getMessage().contains("is not registered"));
    }

    @Test
    void get_withMultipleBeansAndDefaultId_throwsAmbiguousException() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(Service.class, ServiceImpl.class, "one").factory(ServiceImpl::new);
        context.bind(Service.class, ServiceImpl.class, "two").factory(ServiceImpl::new);
        context.initialize();

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.get(Module.DEFAULT_ID, Service.class));

        assertTrue(error.getMessage().contains("There are 2 ids of bean"));
    }

    @Test
    void bind_selfBindingApi_withConcreteClass_succeeds() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(ServiceImpl.class, "impl").factory(ServiceImpl::new);
        context.initialize();

        final ServiceImpl bean = context.get("impl", ServiceImpl.class);

        assertEquals("ok", bean.value());
    }

    @Test
    void bind_selfBindingConsumerApi_withConcreteClass_succeeds() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(ServiceImpl.class, "impl", binding -> binding.factory(ServiceImpl::new));
        context.initialize();

        final ServiceImpl bean = context.get("impl", ServiceImpl.class);

        assertEquals("ok", bean.value());
    }

    @Test
    void get_withUnknownIdAndRegisteredType_throwsException() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(Service.class, ServiceImpl.class, "one").factory(ServiceImpl::new);
        context.initialize();

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.get("missing", Service.class));

        assertTrue(error.getMessage().contains("There are 1 ids of bean"));
    }

    @Test
    void lifecycleMethods_onEmptyContext_areNoop() {
        final ListableContextImpl context = new ListableContextImpl();

        context.initialize();
        context.initializeHooks();
        context.postInitialize();
        context.onReady();
        context.down();
    }

    @Test
    void bind_withClassTypeInInterfaceApi_throwsException() {
        final ListableContextImpl context = new ListableContextImpl();

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.bind(ServiceImpl.class, ServiceImpl.class, "id"));

        assertTrue(error.getMessage().contains("must be an interface"));
    }

    @Test
    void bind_withInterfaceInSelfBindApi_throwsException() {
        final ListableContextImpl context = new ListableContextImpl();

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.bind(Service.class, "id", binding -> binding.factory(ServiceImpl::new)));

        assertTrue(error.getMessage().contains("is an interface"));
    }

    @Test
    void bind_duplicateIdInConsumerBinding_throwsException() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(Service.class, ServiceImpl.class, "one", binding -> binding.factory(ServiceImpl::new));

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.bind(Service.class, ServiceImpl.class, "one", binding -> binding.factory(ServiceImpl::new)));

        assertTrue(error.getMessage().contains("is registered"));
    }

    @Test
    void listAll_forMissingType_returnsEmptyCollection() {
        final ListableContextImpl context = new ListableContextImpl();

        final Collection<Service> services = context.listAll(Service.class);

        assertTrue(services.isEmpty());
    }

    @Test
    void listAll_returnsImmutableCollection() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(Service.class, ServiceImpl.class, "a").factory(ServiceImpl::new);
        context.initialize();

        final Collection<Service> services = context.listAll(Service.class);

        assertThrows(UnsupportedOperationException.class, () -> services.add(new ServiceImpl()));
    }

    @Test
    void initialize_sealsBindings() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(Service.class, ServiceImpl.class, "a").factory(ServiceImpl::new);
        context.initialize();

        final BeanCreationException error = assertThrows(BeanCreationException.class,
                () -> context.bind(Service.class, ServiceImpl.class, "b"));

        assertTrue(error.getMessage().contains("immutable after initialization"));
    }

    @Test
        void initialize_withCircularDependency_throwsException() {
        final ListableContextImpl context = new ListableContextImpl();

        context.bind(A.class, AImpl.class, "a")
                .factory(() -> new AImpl(context.get(Module.DEFAULT_ID, B.class)));
        context.bind(B.class, BImpl.class, "b")
                .factory(() -> new BImpl(context.get(Module.DEFAULT_ID, A.class)));

        final BeanCreationException error = assertThrows(BeanCreationException.class,
            context::initialize);

        assertTrue(hasMessageInChain(error, "Circular dependency detected"));
    }

    private static boolean hasMessageInChain(Throwable error, String fragment) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(fragment)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    interface Service {
        String value();
    }

    static final class ServiceImpl implements Service {
        @Override
        public String value() {
            return "ok";
        }
    }

    interface A {
        B b();
    }

    interface B {
        A a();
    }

    static final class AImpl implements A {
        private final B b;

        AImpl(B b) {
            this.b = b;
        }

        @Override
        public B b() {
            return b;
        }
    }

    static final class BImpl implements B {
        private final A a;

        BImpl(A a) {
            this.a = a;
        }

        @Override
        public A a() {
            return a;
        }
    }
}
