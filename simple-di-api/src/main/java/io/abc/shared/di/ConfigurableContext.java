package io.abc.shared.di;

import java.util.function.Consumer;

/**
 * Mutable context used while configuring container bindings.
 */
public interface ConfigurableContext extends AppContext {

    /**
     * Register a typed binding and configure it through the provided callback.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param id bean identifier
     * @param binding binding configurator
     * @param <T> exposed bean type
     * @param <I> implementation type
     */
    <T, I extends T> void bind(Class<T> type, Class<I> impl, String id, Consumer<Binding<T, I>> binding);

    /**
     * Register a typed binding and return the fluent binding object.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param id bean identifier
     * @param <T> exposed bean type
     * @param <I> implementation type
     * @return created binding
     */
    <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl, String id);

    /**
     * Register a self-binding and configure it through the provided callback.
     *
     * @param type bean type
     * @param id bean identifier
     * @param binding binding configurator
     * @param <T> bean type
     */
    <T> void bind(Class<T> type, String id, Consumer<Binding<T, T>> binding);

    /**
     * Register a self-binding and return the fluent binding object.
     *
     * @param type bean type
     * @param id bean identifier
     * @param <T> bean type
     * @return created binding
     */
    <T> Binding<T, T> bind(Class<T> type, String id);

    /**
     * Register a self-binding with the default identifier.
     *
     * @param type bean type
     * @param <T> bean type
     * @return created binding
     */
    default <T> Binding<T, T> bind(Class<T> type) {
        return bind(type, null);
    }
}
