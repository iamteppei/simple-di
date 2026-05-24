package io.abc.shared.di;

import java.util.List;

/**
 * Entry point to start and stop the dependency injection container.
 */
public interface Container {

    /**
     * Start the container with the provided modules and runtime arguments.
     *
     * @param modules modules to configure
     * @param args runtime arguments
     * @return running application context
     */
    AppContext up(List<ConfigurableModule> modules, String[] args);

    /**
     * Stop the container and release managed resources.
     */
    void down();
}
