package org.myjtools.jexten;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * This interface provides a stream of {@link ModuleLayer} that would be
 * used to locate candidate classes for extensions. It has to be passed to the
 * extension manager in the moment of creation (via {@link ExtensionManager#create(ModuleLayerProvider)})
 * <p>
 * Unless you are using an advance multi-layer module architecture,
 * the org.myjtools.jexten.internal implementation of this interface suffices for most common usages
 * and no client implementation is required.
 */
public interface ModuleLayerProvider {


    static ModuleLayerProvider boot() {
        return () -> Stream.of(ModuleLayer.boot());
    }

    static ModuleLayerProvider empty() {
        return () -> Stream.of(ModuleLayer.empty());
    }

    static ModuleLayerProvider compose(ModuleLayerProvider... moduleLayerProviders) {
        return () -> Stream.of(moduleLayerProviders).flatMap(ModuleLayerProvider::moduleLayers);
    }

    Stream<ModuleLayer> moduleLayers();

    /*
     * This method is used to locate a class by its name
     * and return the first found class in the module layers.
     * @param className the name of the class to locate
     * @return the class if found, or null if not found
     */
    default Class<?> getClass(String className) {
        return moduleLayers()
            .flatMap(it -> it.modules().stream())
            .map(module -> Class.forName(module,className))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

}