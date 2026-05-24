package io.abc.shared.di.core;

public interface Factory {

    String getImplBeanName();

    void onContextInitialize();

    void onPostContextInitialize();

    void onContextReady();

    void onContextDestroy();

    <B> B create(Class<B> type);

    void onInitializeHooks();
}
