package io.abc.shared.di.core;

/**
 * @author tam.nguyen
 * Created on 19-Dec-2020
 */
@FunctionalInterface
public interface HookFunc<T> {

    void apply(T instance);
}
