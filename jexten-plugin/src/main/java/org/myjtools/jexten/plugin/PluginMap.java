package org.myjtools.jexten.plugin;

import org.myjtools.jexten.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginMap {




    record PluginModuleLayer(Plugin plugin, ModuleLayer moduleLayer) {   }

    private final ClassLoader parentClassLoader;
    private final Map<PluginID, Plugin> pluginsByID = new HashMap<>();
    private final Map<PluginManifest,ModuleLayer> layersByPlugin = new HashMap<>();
    private ModuleLayerTree moduleLayerTree;
    private boolean invalidated = true;

    public PluginMap(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    public Optional<Plugin> get(PluginID id) {
        return Optional.ofNullable(pluginsByID.get(id));
    }

    public Optional<Version> getVersion(PluginID pluginID) {
        return get(pluginID).map(Plugin::manifest).map(PluginManifest::version);
    }

    public Iterable<PluginID> ids() {
        return pluginsByID.keySet();
    }

    public boolean containsKey(PluginID pluginID) {
        return pluginsByID.containsKey(pluginID);
    }

    public Stream<ModuleLayer> layers() {
        if (invalidated) {
            buildPluginMap();
            invalidated = false;
        }
        return layersByPlugin.values().stream();
    }

    public void clear() {
        pluginsByID.clear();
        layersByPlugin.clear();
        invalidated = true;
    }

    public void add(Plugin plugin) {
        pluginsByID.put(plugin.manifest().id(), plugin);
        invalidated = true;
    }

    public void remove(PluginID pluginID) {
        pluginsByID.remove(pluginID);
        invalidated = true;
    }



    private void buildPluginMap() {
        computeModuleLayers();
        this.moduleLayerTree = new ModuleLayerTree(layersByPlugin);
    }


    private Map<PluginManifest,ModuleLayer> computeModuleLayers() {
        layersByPlugin.clear();
        Set<ModuleLayer> exploredLayers = new HashSet<>();
        ModuleLayer parentLayer = ModuleLayer.boot();
        Map<PluginManifest,ModuleLayer> newLayers;
        do {
            newLayers = computeModuleLayers(parentLayer,pluginsByID.values());
            exploredLayers.add(parentLayer);
            layersByPlugin.putAll(newLayers);
            parentLayer = layersByPlugin.values().stream()
                .filter(it -> !exploredLayers.contains(it))
                .findAny()
                .orElse(null);
        } while (parentLayer != null);
        return Map.copyOf(layersByPlugin);
    }


    private Map<PluginManifest,ModuleLayer> computeModuleLayers(ModuleLayer parentLayer, Collection<Plugin> plugins) {
        return plugins.stream()
            .filter(plugin -> plugin.isHostedBy(parentLayer))
            .map(plugin -> buildModuleLayer(plugin, parentLayer))
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(
                it -> it.plugin.manifest(),
                PluginModuleLayer::moduleLayer
            ));
    }


    private Optional<PluginModuleLayer> buildModuleLayer(Plugin plugin, ModuleLayer parentLayer) {
        return plugin
            .buildModuleLayer(parentLayer, parentClassLoader)
            .map(moduleLayer -> new PluginModuleLayer(plugin, moduleLayer));
    }

}