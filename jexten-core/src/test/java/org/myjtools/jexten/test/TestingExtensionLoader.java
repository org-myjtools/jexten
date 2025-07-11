package org.myjtools.jexten.test;

import java.util.*;

import org.myjtools.jexten.*;

public class TestingExtensionLoader implements ExtensionLoader {

    public static Object lastExtensionLoaded;

    @Override
    public <T> Optional<T> load(ServiceLoader.Provider<T> provider, Scope scope) {
        System.out.println("getting "+scope+" instance using external extension loader");
        try {
            var instance = provider.type().getConstructor().newInstance();
            lastExtensionLoaded = instance;
            return Optional.of(instance);
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }
}
