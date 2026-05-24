package io.abc.shared.di.core;

import io.abc.shared.di.ConfigurableContext;

import java.util.Collection;

public interface ListableContext extends ConfigurableContext {

    <T> Collection<T> listAll(Class<T> type);
}
