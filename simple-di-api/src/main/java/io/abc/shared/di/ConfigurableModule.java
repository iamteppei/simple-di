package io.abc.shared.di;

/**
 * Contract for modules that configure container bindings.
 */
public interface ConfigurableModule {

    /**
     * Provide the module with the mutable configuration context.
     *
     * @param context mutable configuration context
     */
    void setContext(final ConfigurableContext context);

    /**
     * Register module bindings and configuration.
     */
    void configure();
}
