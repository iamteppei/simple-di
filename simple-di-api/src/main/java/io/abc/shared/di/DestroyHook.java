package io.abc.shared.di;

import java.util.function.BiConsumer;

/**
 * Hook invoked when a previously available bean is being destroyed.
 *
 * @param <T> owner bean type
 * @param <V> dependency type being destroyed
 */
public interface DestroyHook<T, V> extends BiConsumer<T, V> {
}
