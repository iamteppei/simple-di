package io.abc.shared.di.core;

import io.abc.shared.di.AppContext;
import io.abc.shared.di.ConfigurableContext;
import io.abc.shared.di.ConfigurableModule;
import io.abc.shared.di.GeneratedConfigurableModule;
import io.abc.shared.di.Module;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleLoaderAndContainerRunnerTest {

    @Test
    void moduleLoader_loadAll_readsServiceLoaderEntries() {
        final List<ConfigurableModule> modules = ModuleLoader.loadAll();

        assertFalse(modules.isEmpty());
        assertTrue(modules.stream().anyMatch(TestAutoModule.class::isInstance));
        assertTrue(modules.stream().anyMatch(TestGeneratedAutoModule.class::isInstance));
    }

    @Test
    void containerStarter_start_withoutExplicitModules_loadsModulesFromServiceLoader() {
        final AppContext context = ContainerRunner.start();

        assertNotNull(context);
        assertEquals("auto", context.get(AutoService.class).name());
        assertEquals("generated", context.get(GeneratedAutoService.class).name());
    }

    @Test
    void containerStarter_start_withArgs_worksWithServiceLoaderModules() {
        final AppContext context = ContainerRunner.start(new String[]{"--dev"});

        assertNotNull(context);
        assertEquals("auto", context.get(AutoService.class).name());
    }

    @Test
    void containerStarter_startContainer_withArgs_returnsRunningContainer() {
        final io.abc.shared.di.Container container = ContainerRunner.startContainer(new String[]{"--prod"});

        assertNotNull(container);
        container.down();
    }

    interface AutoService {
        String name();
    }

    static final class AutoServiceImpl implements AutoService {
        @Override
        public String name() {
            return "auto";
        }
    }

    interface GeneratedAutoService {
        String name();
    }

    static final class GeneratedAutoServiceImpl implements GeneratedAutoService {
        @Override
        public String name() {
            return "generated";
        }
    }

    public static final class TestAutoModule extends Module {
        @Override
        public void configure() {
            bind(AutoService.class, AutoServiceImpl.class, AutoServiceImpl::new);
        }
    }

    public static final class TestGeneratedAutoModule implements GeneratedConfigurableModule {

        private ConfigurableContext context;

        @Override
        public void setContext(ConfigurableContext context) {
            this.context = context;
        }

        @Override
        public void configure() {
            context.bind(GeneratedAutoService.class, GeneratedAutoServiceImpl.class, GeneratedAutoServiceImpl.class.getName())
                    .factory(GeneratedAutoServiceImpl::new);
        }
    }
}
