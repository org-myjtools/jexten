package org.myjtools.jexten.plugin.internal;

import org.myjtools.jexten.plugin.PluginManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class Plugin {

    public static final Logger log = LoggerFactory.getLogger(Plugin.class);

    private final ModuleFinder moduleFinder;
    private final Set<ModuleReference> moduleReferences;
    private final PluginManifest manifest;


    public Plugin(PluginManifest manifest, List<Path> artifactPaths) {
        if (log.isDebugEnabled()) {
            var artifactPathsString = artifactPaths.stream().map(it -> it.getFileName().toString()).toList();
            log.debug("Creating plugin {} with artifacts: {}", manifest.id(), artifactPaths);
        }
        this.manifest = manifest;
        this.moduleFinder = ModuleFinder.of(artifactPaths.toArray(Path[]::new));
        this.moduleReferences = moduleFinder.findAll();
        if (moduleReferences.isEmpty()) {
            log.warn("Plugin {} has no modules to load", manifest.id());
        } else {
            log.debug("Plugin {} has {} modules to load", manifest.id(), moduleReferences.size());
        }
    }

    /**
     * Get the plugin manifest that describes the plugin.
     * @return The plugin manifest
     */
    public PluginManifest manifest() {
        return manifest;
    }


    /**
     * Compute a list with every Java module used by the plugin, excluding the modules
     * that are already used by the parent layer.
     */
    public List<String> moduleNames(ModuleLayer parentLayer) {
        var parentModules = parentLayer.modules().stream().map(Module::getName).toList();
       return moduleReferences
                .stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .filter(name -> !parentModules.contains(name))
                .distinct()
                .toList();
    }



    public boolean isHostedBy(ModuleLayer moduleLayer) {
        return moduleLayer.modules().stream()
            .map(Module::getName)
            .anyMatch(manifest.hostModule()::equals);
    }



    /**
     * Build the Java {@link ModuleLayer} that will be used to load the classes of the plugin.
     * @return Either the module layer or an empty optional if it could not be created for any reason
     */
    public Optional<ModuleLayer> buildModuleLayer(ModuleLayer parentLayer, ClassLoader parentClassLoader) {
        log.debug("building module layer for plugin {} with modules: {}", manifest.id(), moduleNames(parentLayer));
        log.debug("parent module layer has modules: {}", parentLayer.configuration().modules());
        try {
            return Optional.of(parentLayer.defineModulesWithOneLoader(
                parentLayer.configuration().resolve(this.moduleFinder, ModuleFinder.of(), moduleNames(parentLayer)),
                parentClassLoader
            ));
        } catch (RuntimeException e) {
            log.error("Cannot build the module layer of plugin {} : {}", this, e.getMessage(),e);
            return Optional.empty();
        }
    }

    public Set<ModuleReference> moduleReferences() {
        return moduleReferences;
    }


    @Override
    public String toString() {
        return manifest.id().toString();
    }
}
