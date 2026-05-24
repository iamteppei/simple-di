package io.abc.shared.di.core;

import io.abc.shared.di.AppContext;
import io.abc.shared.di.ConfigurableModule;
import io.abc.shared.di.Container;
import io.abc.shared.di.Module;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerRunnerTest {

    @Test
    void start_withModules_returnsContextAndResolvesBean() {
        final AppContext context = ContainerRunner.start(List.of(new TestModule()), new String[]{"arg"});

        final TestService service = context.get(TestService.class);

        assertNotNull(service);
        assertEquals("ok", service.value());
    }

    @Test
    void startContainer_withModules_startsAndCanShutDown() {
        final Container container = ContainerRunner.startContainer(List.of(new TestModule()), null);

        assertNotNull(container);

        container.down();
        // Down is expected to be idempotent.
        container.down();
    }

    @Test
    void start_withNullModules_throwsException() {
        assertThrows(NullPointerException.class, () -> ContainerRunner.start((List<ConfigurableModule>) null, new String[0]));
    }

    @Test
    void startContainer_withNullModules_throwsException() {
        assertThrows(NullPointerException.class, () -> ContainerRunner.startContainer((List<ConfigurableModule>) null, new String[0]));
    }

    interface TestService {
        String value();
    }

    static final class TestServiceImpl implements TestService {
        @Override
        public String value() {
            return "ok";
        }
    }

    static final class TestModule extends Module {
        @Override
        public void configure() {
            bind(TestService.class, TestServiceImpl.class, TestServiceImpl::new);
        }
    }
}
