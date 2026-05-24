package io.abc.shared.di.core;

import io.abc.shared.di.AvailableHook;
import io.abc.shared.di.Binding;
import io.abc.shared.di.DestroyHook;
import io.abc.shared.di.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BindingImpl<T, I extends T> extends Binding<T, I> implements FactoryCreator {

    private final Class<T> type;
    private final Class<I> implType;
    private final ListableContext context;

    private Scope scope = Scope.SINGLETON;
    private Consumer<I> postConstruct;
    private Consumer<I> preDestroy;
    private Consumer<I> postContainerInitialized;
    private Supplier<I> factory;
    private List<HookFunc<I>> availableHooks;
    private List<HookFunc<I>> destroyHooks;

    public BindingImpl(Class<T> type, Class<I> implType, ListableContext context) {
        this.type = type;
        this.context = context;
        this.implType = implType;
    }

    @Override
    public Binding<T, I> usePostContainerInitialized(Consumer<I> func) {
        assertNotNull(func, "usePostContainerInitialized must not be null");
        this.postContainerInitialized = func;
        return this;
    }

    @Override
    public Binding<T, I> usePostConstruct(Consumer<I> func) {
        assertNotNull(func, "PostConstruct must not be null");
        this.postConstruct = func;
        return this;
    }

    @Override
    public Binding<T, I> usePreDestroy(Consumer<I> func) {
        assertNotNull(func, "PreDestroy must not be null");
        this.preDestroy = func;
        return this;
    }

    @Override
    public Binding<T, I> factory(Supplier<I> func) {
        assertNotNull(func, "Bean " + type.getName() + " does not accept a null factory method");
        assertNull(this.factory, "Factory is already set. Override is not allowed");
        this.factory = func;
        return this;
    }

    @Override
    public Binding<T, I> scope(Scope scope) {
        assertNotNull(scope, "Scope must not be null");
        this.scope = scope;
        return this;
    }

    @Override
    public <B> Binding<T, I> useBeanHook(Class<B> type, AvailableHook<I, B> onAvailable, DestroyHook<I, B> onDestroy) {
        if (!type.isInterface()) {
            throw new IllegalStateException(type.getName() + " must be an interface");
        }

        if (onAvailable != null) {
            getOrCreateAvailableHook().add(i -> context.listAll(type).forEach(it -> onAvailable.accept(i, it)));
        }

        if (onDestroy != null) {
            getOrCreateDestroyHook().add(i -> context.listAll(type).forEach(it -> onDestroy.accept(i, it)));
        }
        return this;
    }

    @Override
    public Factory create() {
        return createFactory();
    }

    private Factory createFactory() {
        assertNotNull(factory, "Bean " + type.getName() + " missed to defined a factory method");
        if (Scope.SINGLETON.equals(scope)) {
            return new FactoryTypedSingleton<>(
                    type,
                    implType,
                    postConstruct,
                    preDestroy,
                    factory,
                    availableHooks == null ? null : Collections.unmodifiableList(availableHooks),
                    destroyHooks == null ? null : Collections.unmodifiableList(destroyHooks),
                    postContainerInitialized
            );
        }
        if (Scope.PROTOTYPE.equals(scope)) {
            return new FactoryTypedPrototype<>(
                    type,
                    implType,
                    factory,
                    postConstruct,
                    availableHooks == null ? null : Collections.unmodifiableList(availableHooks)
            );
        }
        throw new IllegalStateException(scope + " is not supported");
    }

    private void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private void assertNull(Object value, String message) {
        if (value != null) {
            throw new IllegalStateException(message);
        }
    }

    private List<HookFunc<I>> getOrCreateAvailableHook() {
        if (this.availableHooks == null) {
            this.availableHooks = new ArrayList<>();
        }
        return this.availableHooks;
    }

    private List<HookFunc<I>> getOrCreateDestroyHook() {
        if (this.destroyHooks == null) {
            this.destroyHooks = new ArrayList<>();
        }
        return this.destroyHooks;
    }
}
