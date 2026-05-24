package io.abc.shared.di;

/**
 * Read-only access to resolved beans from the container.
 */
public interface AppContext {

    /**
     * Resolve a bean by identifier and type.
     *
     * @param id bean identifier
     * @param type expected bean type
     * @param <T> bean type
     * @return resolved bean instance
     */
    <T> T get(String id, Class<T> type);

    /**
     * Resolve a bean from the default identifier.
     *
     * @param type expected bean type
     * @param <T> bean type
     * @return resolved bean instance
     */
    default <T> T get(Class<T> type) {
        return get(Module.DEFAULT_ID, type);
    }
}
