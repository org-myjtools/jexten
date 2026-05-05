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
import java.util.stream.Collectors;


public class Plugin {

    public static final Logger log = LoggerFactory.getLogger(Plugin.class);

    private final ModuleFinder moduleFinder;
    private final Set<ModuleReference> moduleReferences;
    private final PluginManifest manifest;


    public Plugin(PluginManifest manifest, List<Path> artifactPaths) {
        if (log.isDebugEnabled()) {
            var artifactPathsString = artifactPaths.stream().map(it -> it.getFileName().toString()).toList();
            log.debug("Creating plugin {} with artifacts: {}", manifest.id(), artifactPathsString);
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
            // Build a finder that excludes modules already visible in the parent layer, so the
            // resolver picks them from the parent configuration instead of re-adding them to the
            // plugin layer (which would cause "reads more than one module" errors for automatic modules).
            ModuleFinder pluginOnlyFinder = filteredFinder(parentLayer);
            return Optional.of(parentLayer.defineModulesWithOneLoader(
                parentLayer.configuration().resolve(pluginOnlyFinder, ModuleFinder.ofSystem(), moduleNames(parentLayer)),
                parentClassLoader
            ));
        } catch (RuntimeException e) {
            log.error("Cannot build the module layer of plugin {} : {}", this, e.getMessage(),e);
            return Optional.empty();
        }
    }

    private ModuleFinder filteredFinder(ModuleLayer parentLayer) {
        var parentModuleNames = parentLayer.modules().stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        var filteredRefs = moduleReferences.stream()
            .filter(ref -> !parentModuleNames.contains(ref.descriptor().name()))
            .collect(Collectors.toSet());
        return new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                if (parentModuleNames.contains(name)) {
                    return Optional.empty();
                }
                return moduleFinder.find(name);
            }
            @Override
            public Set<ModuleReference> findAll() {
                return filteredRefs;
            }
        };
    }

    public Set<ModuleReference> moduleReferences() {
        return moduleReferences;
    }


    @Override
    public String toString() {
        return manifest.id().toString();
    }
}
