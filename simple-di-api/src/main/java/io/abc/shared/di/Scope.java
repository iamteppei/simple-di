package io.abc.shared.di;

/**
 * Bean lifecycle scopes supported by the container.
 */
public enum Scope {
    /** One shared instance per container lifetime. */
    SINGLETON,
    /** One shared instance per plugin/runtime partition. */
    PLUGIN,
    /** A new instance is created for each resolution. */
    PROTOTYPE
}
