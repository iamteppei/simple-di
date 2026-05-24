package io.abc.shared.di;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base class for modules that declare bindings against a {@link ConfigurableContext}.
 */
public abstract class Module implements GeneratedConfigurableModule {

    /** Default bean identifier used for unnamed bindings. */
    public static final String DEFAULT_ID = "";

    private ConfigurableContext ctx;

    /**
     * Set module context used during configuration.
     *
     * @param context mutable configuration context
     */
    @Override
    public final void setContext(final ConfigurableContext context) {
        ctx = context;
    }

    /**
     * Bind an implementation to a type and configure it.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param id bean identifier
     * @param config binding configurator
     * @param <T> exposed bean type
     * @param <I> implementation type
     */
    protected final <T, I extends T> void bind(final Class<T> type, final Class<I> impl, final String id, final Consumer<Binding<T, I>> config) {
        ctx.bind(type, impl, id, config);
    }

    /**
     * Bind an implementation to a type using implementation class name as identifier.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param config binding configurator
     * @param <T> exposed bean type
     * @param <I> implementation type
     */
    protected final <T, I extends T> void bind(final Class<T> type, final Class<I> impl, final Consumer<Binding<T, I>> config) {
        bind(type, impl, impl.getName(), config);
    }

    /**
     * Bind an implementation to a type.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param id bean identifier
     * @param <T> exposed bean type
     * @param <I> implementation type
     * @return created binding
     */
    protected final <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl, String id) {
        return ctx.bind(type, impl, id);
    }

    /**
     * Bind an implementation to a type using a custom factory.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param id bean identifier
     * @param factory bean factory
     * @param <T> exposed bean type
     * @param <I> implementation type
     * @return created binding
     */
    protected final <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl, String id, Supplier<I> factory) {
        return ctx.bind(type, impl, id).factory(factory);
    }

    /**
     * Bind an implementation to a type using implementation class name as identifier.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param <T> exposed bean type
     * @param <I> implementation type
     * @return created binding
     */
    protected final <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl) {
        return bind(type, impl, impl.getName());
    }

    /**
     * Bind a type to itself using the default identifier.
     *
     * @param type bean type
     * @param <T> bean type
     * @return created binding
     */
    protected final <T> Binding<T, T> bind(Class<T> type) {
        return ctx.bind(type);
    }

    /**
     * Bind an implementation to a type using a custom factory and implementation class name as identifier.
     *
     * @param type exposed bean type
     * @param impl implementation class
     * @param factory bean factory
     * @param <T> exposed bean type
     * @param <I> implementation type
     * @return created binding
     */
    protected final <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl, Supplier<I> factory) {
        return bind(type, impl, impl.getName(), factory);
    }

    /**
     * Resolve a bean from the context by identifier and type.
     *
     * @param id bean identifier
     * @param type expected bean type
     * @param <T> bean type
     * @return resolved bean instance
     */
    protected <T> T get(final String id, final Class<T> type) {
        return ctx.get(id, type);
    }

    /**
     * Resolve a bean from the context using the default identifier.
     *
     * @param type expected bean type
     * @param <T> bean type
     * @return resolved bean instance
     */
    protected <T> T get(final Class<T> type) {
        return ctx.get(DEFAULT_ID, type);
    }
}
