package io.abc.shared.di;

import java.util.function.BiConsumer;

/**
 * Hook invoked when another bean becomes available.
 *
 * @param <T> owner bean type
 * @param <V> available dependency type
 */
public interface AvailableHook<T, V> extends BiConsumer<T, V> {
}
