package org.myjtools.jexten.plugin;


import org.myjtools.jexten.plugin.internal.ModuleLayerTreeToStringVisitor;

import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;



/**
 * A plugin module layer tree is a structure that describes the module layer
 * composition regarding a set of plugins. Each node
 */
public class ModuleLayerTree {


    private static final Pattern JAVA_MODULE = Pattern.compile("(jdk|java|javax)\\..*");

    private final ModuleLayerTree parent; // null for boot layer
    private final ModuleLayer moduleLayer;
    private final List<Module> modules;
    private final PluginManifest plugin; // null for boot layer
    private final List<ModuleLayerTree> children;
    private final int depth;


    public ModuleLayerTree(Map<PluginManifest,ModuleLayer> pluginMap) {
        this(null,ModuleLayer.boot(),null,0,pluginMap);
    }


    private ModuleLayerTree(
        ModuleLayerTree parent,
        ModuleLayer moduleLayer,
        PluginManifest plugin,
        int depth,
        Map<PluginManifest,ModuleLayer> pluginMap
    ) {
        this.parent = parent;
        this.moduleLayer = moduleLayer;
        this.modules = moduleLayer.modules().stream()
            .filter(it -> !JAVA_MODULE.matcher(it.getName()).matches())
            .sorted(Comparator.comparing(Module::getName))
            .toList();
        this.plugin = plugin;
        this.depth = depth;
        this.children =
            pluginMap.entrySet().stream()
                    .filter(e -> e.getValue().parents().contains(moduleLayer))
                    .map(e -> new ModuleLayerTree(this, e.getValue(), e.getKey(), depth+1,pluginMap))
                    .toList();
    }


    public Optional<ModuleLayerTree> parent() {
        return Optional.ofNullable(parent);
    }


    public ModuleLayer moduleLayer() {
        return moduleLayer;
    }


    /** @return
     * An immutable list of non-Java modules existing in this layer
     * */
    public List<Module> modules() {
        return modules;
    }


    /** @return
     *  The plugin that generated this layer, or <code>empty</code> if it is the boot layer
     *  */
    public Optional<PluginManifest> plugin() {
        return Optional.ofNullable(plugin);
    }


    public int depth() {
        return depth;
    }


    /**
     * @return
     * A stream visiting each child node of the tree, starting by this object
     */
    public Stream<ModuleLayerTree> stream() {
        return Stream.concat(
            Stream.of(this),
            children.stream().flatMap(ModuleLayerTree::stream)
        );
    }


    public void forEach(Consumer<ModuleLayerTree> visitor) {
        visitor.accept(this);
        children.forEach(it -> it.forEach(visitor));
    }

    public void forEach(ModuleLayerTreeVisitor visitor) {
        visitor.enterLayer(moduleLayer, plugin, depth);
        modules.forEach(module -> visitor.visitModule(moduleLayer, plugin, depth, module));
        children.forEach(child -> child.forEach(visitor));
        visitor.exitLayer(moduleLayer, plugin, depth);
    }


    public String description() {
        return ModuleLayerTreeToStringVisitor.toString(this);
    }

}
