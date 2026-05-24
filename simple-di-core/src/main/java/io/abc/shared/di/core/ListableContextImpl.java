package io.abc.shared.di.core;

import io.abc.shared.di.BeanCreationException;
import io.abc.shared.di.Binding;
import io.abc.shared.di.Module;

import java.util.*;
import java.util.function.Consumer;

public final class ListableContextImpl implements ListableContext {

    private final Map<Class<?>, Map<String, FactoryCreator>> beanFactoryCreatorMap = new HashMap<>();
    private final ThreadLocal<LinkedHashSet<String>> instantiating = ThreadLocal.withInitial(LinkedHashSet::new);
    private Map<Class<?>, Map<String, Factory>> beanMap = new HashMap<>();
    private boolean bindingsSealed;

    @Override
    public <T> T get(String id, Class<T> type) {

        final Map<String, Factory> beans = this.beanMap.get(type);
        if (beans != null) {
            Factory factory = beans.get(id);
            if (factory == null && id.equals(Module.DEFAULT_ID) && beans.size() == 1) {
                factory = beans.values().iterator().next();
            }
            if (factory != null) {
                return safeCreation(type, factory);
            }
            throw new BeanCreationException("There are " + beans.size() + " ids of bean " + type.getName() + ". Ids " + String.join(",", beans.keySet()));
        }
        throw new BeanCreationException("Bean " + type.getName() + " with id \"" + id + "\" is not registered");
    }

    @Override
    public <T> Binding<T, T> bind(Class<T> type, String id) {
        throwIfBindingsSealed();
        return doBind(type, type, id);
    }

    @Override
    public <T> void bind(Class<T> type, String id, Consumer<Binding<T, T>> binding) {
        throwIfBindingsSealed();
        throwIfInterface(type);
        doBind(type, type, id, binding);
    }

    @Override
    public <T, I extends T> void bind(Class<T> type, Class<I> impl, String id, Consumer<Binding<T, I>> binding) {
        throwIfBindingsSealed();
        throwIfNotAnInterface(type);
        doBind(type, impl, id, binding);
    }

    private <T, I extends T> void doBind(Class<T> type, Class<I> impl, String id, Consumer<Binding<T, I>> binding) {
        final Map<String, Factory> beanFactories = beanMap.computeIfAbsent(type, it -> new LinkedHashMap<>());
        final String _id = Optional.ofNullable(id).orElse(impl.getName());
        if (beanFactories.containsKey(_id)) {
            throw new BeanCreationException("Bean " + type.getName() + " with id \"" + _id + "\" is registered");
        }
        final BindingImpl<T, I> bindingImpl = new BindingImpl<>(type, impl, this);
        binding.accept(bindingImpl);
        beanFactories.put(_id, bindingImpl.create());
    }

    @Override
    public <T, I extends T> Binding<T, I> bind(Class<T> type, Class<I> impl, String id) {
        throwIfBindingsSealed();
        throwIfNotAnInterface(type);
        return doBind(type, impl, id);
    }

    private void throwIfBindingsSealed() {
        if (bindingsSealed) {
            throw new BeanCreationException("Context bindings are immutable after initialization");
        }
    }

    private <T, I extends T> Binding<T, I> doBind(Class<T> type, Class<I> impl, String id) {
        final Map<String, FactoryCreator> factoryCreators = beanFactoryCreatorMap.computeIfAbsent(type, it -> new LinkedHashMap<>());
        final BindingImpl<T, I> binding = new BindingImpl<>(type, impl, this);
        factoryCreators.put(Optional.ofNullable(id).orElse(impl.getName()), binding);
        return binding;
    }

    private <T> void throwIfInterface(Class<T> type) {
        if (type.isInterface()) {
            throw new BeanCreationException(type.getName() + " is an interface");
        }
    }

    private <T> void throwIfNotAnInterface(Class<T> type) {
        if (!type.isInterface()) {
            throw new BeanCreationException(type.getName() + " must be an interface");
        }
    }

    @Override
    public <T> Collection<T> listAll(Class<T> type) {
        final Map<String, Factory> beans = this.beanMap.get(type);
        if (beans != null) {
            final List<T> instances = new ArrayList<>(beans.size());
            for (Factory factory : beans.values()) {
                instances.add(safeCreation(type, factory));
            }
            return Collections.unmodifiableList(instances);
        }
        return Collections.emptyList();
    }

    private <T> T safeCreation(Class<T> type, Factory factory) {
        final LinkedHashSet<String> stack = instantiating.get();
        final String internalName = factory.getImplBeanName();
        try {
            if (stack.contains(internalName)) {
                throw new BeanCreationException("Circular dependency detected: " + buildCycleTrace(stack, internalName));
            }
            stack.add(internalName);
            return factory.create(type);
        } finally {
            stack.remove(internalName);
            if (stack.isEmpty()) {
                instantiating.remove();
            }
        }
    }

    public void down() {
        if (!beanMap.isEmpty()) {
            final Map<Class<?>, Map<String, Factory>> map = new HashMap<>(beanMap);
            beanMap = Collections.emptyMap();
            for (Map<String, Factory> bean : map.values()) {
                for (Factory value : bean.values()) {
                    value.onContextDestroy();
                }
            }
        }
    }

    public void initialize() {
        bindingsSealed = true;
        if (!beanFactoryCreatorMap.isEmpty()) {
            refresh();
        }

        if (!beanMap.isEmpty()) {
            for (Map<String, Factory> factoryMap : beanMap.values()) {
                for (Factory factory : factoryMap.values()) {
                    factory.onContextInitialize();
                }
            }
        }

        beanMap = toImmutableBeanMap(beanMap);
    }

    private void refresh() {
        for (final Map.Entry<Class<?>, Map<String, FactoryCreator>> entry : beanFactoryCreatorMap.entrySet()) {
            final Class<?> beanType = entry.getKey();
            final Map<String, Factory> beanFactories = beanMap.computeIfAbsent(beanType, it -> new LinkedHashMap<>());
            Optional.ofNullable(entry.getValue()).ifPresent(factories -> {
                for (final Map.Entry<String, FactoryCreator> factory : factories.entrySet()) {
                    beanFactories.put(factory.getKey(), factory.getValue().create());
                }
            });
        }
        beanFactoryCreatorMap.clear();
    }

    private Map<Class<?>, Map<String, Factory>> toImmutableBeanMap(Map<Class<?>, Map<String, Factory>> source) {
        final Map<Class<?>, Map<String, Factory>> immutable = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, Map<String, Factory>> entry : source.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(immutable);
    }

    public void postInitialize() {
        if (!beanMap.isEmpty()) {
            for (Map<String, Factory> factoryMap : beanMap.values()) {
                for (Factory factory : factoryMap.values()) {
                    factory.onPostContextInitialize();
                }
            }
        }
    }

    private String buildCycleTrace(Set<String> stack, String current) {
        List<String> path = new ArrayList<>(stack);
        path.add(current);
        return String.join(" -> ", path);
    }

    public void onReady() {
        if (!beanMap.isEmpty()) {
            for (Map<String, Factory> factoryMap : beanMap.values()) {
                for (Factory factory : factoryMap.values()) {
                    factory.onContextReady();
                }
            }
        }
    }

    public void initializeHooks() {
        if (!beanMap.isEmpty()) {
            for (Map<String, Factory> factoryMap : beanMap.values()) {
                for (Factory factory : factoryMap.values()) {
                    factory.onInitializeHooks();
                }
            }
        }
    }
}
