package org.myjtools.jexten;

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

}