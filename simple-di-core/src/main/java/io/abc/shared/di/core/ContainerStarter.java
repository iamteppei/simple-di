package io.abc.shared.di.core;

import io.abc.shared.di.AppContext;
import io.abc.shared.di.ConfigurableModule;
import io.abc.shared.di.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public final class ContainerStarter {

    private static final Logger logger = LoggerFactory.getLogger(ContainerStarter.class);

    private ContainerStarter() {
        // utility class
    }

    public static AppContext start() {
        return start(new String[0]);
    }

    public static AppContext start(String[] args) {
        return start(ModuleLoader.loadAll(), args);
    }

    public static AppContext start(List<ConfigurableModule> modules, String[] args) {
        Objects.requireNonNull(modules, "modules must not be null");
        final ContainerImpl container = new ContainerImpl();
        logger.info(">> Starting container with [{}] module(s)", modules.size());
        return container.up(modules, normalizeArgs(args));
    }

    public static Container startContainer(String[] args) {
        return startContainer(ModuleLoader.loadAll(), args);
    }

    public static Container startContainer(List<ConfigurableModule> modules, String[] args) {
        Objects.requireNonNull(modules, "modules must not be null");
        final ContainerImpl container = new ContainerImpl();
        logger.info(">> Starting container with [{}] module(s)", modules.size());
        container.up(modules, normalizeArgs(args));
        return container;
    }

    private static String[] normalizeArgs(String[] args) {
        return args == null ? new String[0] : args.clone();
    }

}
