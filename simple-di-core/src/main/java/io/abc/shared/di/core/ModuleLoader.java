package io.abc.shared.di.core;

import io.abc.shared.di.ConfigurableModule;
import io.abc.shared.di.GeneratedConfigurableModule;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ModuleLoader {

    private ModuleLoader() {
        //
    }

    public static List<ConfigurableModule> loadAll() {
        final List<ConfigurableModule> modules = new ArrayList<>();
        modules.addAll(loadConfigurableModules(ConfigurableModule.class));
        modules.addAll(loadConfigurableModules(GeneratedConfigurableModule.class));
        return modules;
    }

    private static <M extends ConfigurableModule> List<M> loadConfigurableModules(Class<M> type) {
        return StreamSupport
                .stream(ServiceLoader.load(type).spliterator(), false)
                .collect(Collectors.toList());
    }
}
