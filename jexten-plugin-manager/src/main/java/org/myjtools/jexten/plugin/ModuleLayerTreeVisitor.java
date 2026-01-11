package org.myjtools.jexten.plugin;

public interface ModuleLayerTreeVisitor {

    void enterLayer(ModuleLayer layer, PluginManifest plugin, int depth);

    void exitLayer(ModuleLayer layer, PluginManifest plugin, int depth);

    void visitModule(ModuleLayer layer, PluginManifest plugin, int depth, Module module);


}
