package io.abc.shared.di;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fluent API for configuring a bean binding in the container.
 *
 * @param <T> exposed bean type
 * @param <I> concrete implementation type
 */
public abstract class Binding<T, I extends T> {

    /**
     * Register a lifecycle function that will be triggered after the bean is created.
     * FactoryTypedSingleton
     *
     * @param func the function
     * @return the current {@link Binding}
     */
    public abstract Binding<T, I> usePostConstruct(Consumer<I> func);

    /**
     * Register a lifecycle function that will be triggered before the bean is destroyed
     *
     * @param func the function
     * @return the current {@link Binding}
     */
    public abstract Binding<T, I> usePreDestroy(Consumer<I> func);

    /**
     * Register a lifecycle hook that will be triggered after the container is initialized
     * <p>
     * The hook is not invoked when a bean is lazy initialized or having {@link Scope#PROTOTYPE} scope
     *
     * @param func the function
     * @return the current {@link Binding}
     */
    public abstract Binding<T, I> usePostContainerInitialized(Consumer<I> func);

    /**
     * Set a factory used to create bean instances.
     *
     * @param func the factory function
     * @return the current {@link Binding}
     */
    public abstract Binding<T, I> factory(Supplier<I> func);

    /**
     * Configure bean lifecycle scope.
     *
     * @param scope target scope
     * @return the current {@link Binding}
     */
    public abstract Binding<T, I> scope(Scope scope);

    /**
     * Register hooks for when another bean of the given type is available and when it is destroyed.
     *
     * @param type dependency type to observe
     * @param onAvailable callback invoked when a matching dependency is available
     * @param onDestroy callback invoked when a matching dependency is destroyed
     * @param <B> dependency type
     * @return the current {@link Binding}
     */
    public abstract <B> Binding<T, I> useBeanHook(Class<B> type, AvailableHook<I, B> onAvailable, DestroyHook<I, B> onDestroy);
}
