package io.abc.shared.di.core;

import io.abc.shared.di.BeanCreationException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FactoryTypedPrototype<T, IMPL extends T> implements FactoryTyped<T, IMPL> {

    private final Class<T> type;
    private final Class<IMPL> implType;
    private final Supplier<IMPL> factory;
    private final Consumer<IMPL> postConstruct;
    private final List<HookFunc<IMPL>> availableHooks;

    public FactoryTypedPrototype(Class<T> type, Class<IMPL> implType, Supplier<IMPL> factory, Consumer<IMPL> postConstruct, List<HookFunc<IMPL>> availableHooks) {
        this.type = type;
        this.implType = implType;
        this.factory = factory;
        this.postConstruct = postConstruct;
        this.availableHooks = availableHooks;
    }

    @Override
    public String getImplBeanName() {
        return implType.getName();
    }

    @Override
    public <B> B create(Class<B> type) {
        if (this.type.equals(type)) {
            try {
                final IMPL impl = factory.get();
                // available hooks before post construct
                if (availableHooks != null) {
                    for (final HookFunc<IMPL> availableHook : availableHooks) {
                        availableHook.apply(impl);
                    }
                }
                // post construct after all hooks
                if (this.postConstruct != null) {
                    postConstruct.accept(impl);
                }
                return type.cast(impl);
            } catch (Exception exp) {
                throw new BeanCreationException("Fail creation of bean " + implType.getName() + " with prototype scope", exp);
            }
        }
        throw new BeanCreationException(type.getName() + " is invalid. Expected " + this.type.getName());
    }

    @Override
    public void onContextReady() {
        // do not support
    }

    @Override
    public void onPostContextInitialize() {
        // do not support
    }

    @Override
    public void onContextDestroy() {
        // do not support
    }

    @Override
    public void onContextInitialize() {
        // do nothing
    }

    @Override
    public void onInitializeHooks() {
        // do nothing
    }
}
