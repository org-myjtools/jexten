package org.myjtools.jexten.plugin;

import org.myjtools.jexten.ModuleLayerProvider;
import org.myjtools.jexten.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.myjtools.jexten.plugin.PluginBundleFile.findArtifactName;
import static org.myjtools.jexten.plugin.PluginBundleFile.findArtifactVersion;

public class PluginManager implements ModuleLayerProvider {




    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final String application;
    private final Path artifactDirectory;
    private final Path manifestDirectory;
    private final PluginMap pluginMap;
    private ArtifactStore artifactStore;




    public PluginManager(String application, ClassLoader parentClassLoader, Path pluginDirectory) {
        try {
            log.info("Preparing PluginManager for {} using directory {}",application,pluginDirectory);
            this.application = application;
            this.pluginMap = new PluginMap(parentClassLoader);
            this.artifactDirectory = pluginDirectory.resolve("artifacts");
            this.manifestDirectory = pluginDirectory.resolve("manifests");
            Files.createDirectories(artifactDirectory);
            Files.createDirectories(manifestDirectory);
            discoverInstalledPlugins();
        } catch (Exception e) {
            throw PluginException.wrapper(e);
        }
    }

    private void discoverInstalledPlugins() {
        pluginMap.clear();
        try (var walker = Files.walk(manifestDirectory)) {
            walker.forEach(this::discoverInstalledPlugin);
        } catch (IOException e) {
            throw new PluginException(e,"Error discovering installed plugins");
        }
    }

    private void discoverInstalledPlugin(Path path) {
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                PluginManifest plugin = PluginManifest.read(reader);
                if (!validatePluginApplication(plugin)) {
                    return;
                }
                checkArtifacts(plugin);
                pluginMap.add(buildPlugin(plugin));
                log.info("Plugin {} {} prepared.", plugin.id(), plugin.version());
            } catch (Exception e) {
                log.error("Error reading plugin manifest file {}", path, e);
            }
        }
    }



    /**
     * Refresh the plugin manager contents, reloading all installed plugins.
     * This will clear the current plugin list and rediscover all plugins in the manifest directory.
     */
    public void refresh() {
        log.info("Refreshing Plugin Manager contents");
        pluginMap.clear();
        discoverInstalledPlugins();
    }


    @Override
    public Stream<ModuleLayer> moduleLayers() {
        return pluginMap.layers();
    }


    /**
     * Install a plugin bundle from a file. The bundle file must contain a valid plugin manifest.
     * If the plugin is already installed, it will be updated if the new version is greater than the existing one.
     * @param bundleFile The path to the plugin bundle file
     */
    public void installPluginFromBundle(Path bundleFile) {
        try {
            PluginBundleFile bundle = PluginBundleFile.read(bundleFile);
            PluginManifest manifest = bundle.plugin();
            PluginID pluginID = manifest.id();
            Path manifestFile = manifestPath(pluginID);
            bundle.extract(artifactDirectory);
            installPluginManifest(manifest,manifestFile);
        } catch (IOException  e) {
            throw new PluginException(e, "Cannot install plugin from file {}", bundleFile);
        }
    }


    /**
     * Install a plugin from a JAR file. The JAR file must contain a valid plugin manifest.
     * If the plugin is already installed, it will be updated if the new version is greater than the existing one.
     * It also retrieves the plugin dependencies and copies them to the artifacts directory.
     * @param jarFile The path to the plugin JAR file
     */
    public void installPluginFromJar(Path jarFile) {
        try {
            PluginJarFile bundle = PluginJarFile.read(jarFile);
            PluginManifest manifest = bundle.plugin();
            if (!validatePluginApplication(manifest)) {
                return;
            }
            Path manifestFile = manifestPath(manifest.id());
            installPluginManifest(manifest,manifestFile);
            installArtifact(manifest.group(),jarFile);
            retrieveDependencies(manifest); // Copy the jar file to the artifacts directory
        } catch (IOException  e) {
            throw new PluginException(e, "Cannot install plugin from file {}", jarFile);
        }
    }


    private void installPluginManifest(PluginManifest manifest, Path manifestFile) throws IOException {

        PluginID pluginID = manifest.id();
        Version candidateVersion = manifest.version();

        if (!this.application.equals(manifest.application())) {
            throw new PluginException(
                "Plugin {} is not compatible with this application (expected {}, found {})",
                pluginID,
                this.application,
                manifest.application()
            );
        }

        if (pluginMap.containsKey(pluginID)) {
            Version existingVersion = pluginMap.getVersion(pluginID).orElseThrow();
            if (existingVersion.compareTo(candidateVersion) >= 0) {
                log.warn("Plugin {} already present with version {}", pluginID, existingVersion);
            } else {
                log.info("Updating plugin {} from version {} to {}", pluginID, existingVersion, candidateVersion);
                Files.deleteIfExists(manifestFile);
                pluginMap.remove(pluginID);
            }
        } else {
            log.info("Installing plugin {} version {}", pluginID, candidateVersion);
        }

        try (Writer writer = Files.newBufferedWriter(manifestFile)) {
            manifest.write(writer);
        }
        pluginMap.add(buildPlugin(manifest));
    }


    private void installArtifact(String group, Path jarFile) {
        String name = findArtifactName(jarFile);
        String version = findArtifactVersion(jarFile);
        Path newPath = artifactDirectory
            .resolve(group)
            .resolve(name)
            .resolve(version)
            .resolve(jarFile.getFileName());
        try {
            Files.createDirectories(newPath.getParent());
            Files.copy(jarFile, newPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Installed artifact {} in {}", jarFile.getFileName(), newPath);
        } catch (IOException e) {
            throw new PluginException(e, "Cannot install artifact {} in {}", jarFile, newPath);
        }
    }


    private void retrieveDependencies(PluginManifest manifest) {
        if (artifactStore == null) {
            throw new PluginException(
                "Artifact store is not set, cannot retrieve dependencies for plugin {}",
                manifest.id()
            );
        }
        Map<String, List<Path>> retrievedArtifacts = artifactStore.retrieveArtifacts(manifest.artifacts());
        retrievedArtifacts.forEach((group, artifacts) -> {
            artifacts.forEach(artifact -> {
                installArtifact(group, artifact);
            });
        });
    }


    /**
     * Remove a plugin from the manager. The plugin must be installed.
     * @param pluginID The ID of the plugin to remove
     */
    public void removePlugin(PluginID pluginID) {
        if (!pluginMap.containsKey(pluginID)) {
            throw new PluginException("Plugin {} is not present", pluginID);
        }
        try {
            pluginMap.remove(pluginID);
            Files.deleteIfExists(manifestPath(pluginID));
            log.info("Removed plugin {}", pluginID);
        } catch (IOException e) {
            throw new PluginException(e, "Cannot remove plugin {}", pluginID);
        }
    }

    /**
     * Set the artifact store for this plugin manager.
     * The artifact store is used to retrieve artifacts for plugins.
     * @param artifactStore The artifact store to set
     */
    public void setArtifactStore(ArtifactStore artifactStore) {
        this.artifactStore = artifactStore;
    }


    /**
     * Get the artifact store for this plugin manager.
     * The artifact store is used to retrieve artifacts for plugins.
     * @return The artifact store
     */
    public ArtifactStore artifactStore() {
        return artifactStore;
    }


    /**
     * Get the application name for which this plugin manager is configured.
     * @return The application name
     */
    public String application() {
        return application;
    }


    public Optional<PluginManifest> getPluginManifest(PluginID pluginID) {
        return pluginMap.get(pluginID).map(Plugin::manifest);
    }


    private Path manifestPath(PluginID pluginID) {
        return manifestDirectory.resolve(pluginID.group() + "-" + pluginID.name() + ".yaml");
    }



    private Plugin buildPlugin(PluginManifest plugin) {
        List<Path> artifactPaths = plugin.artifacts().entrySet().stream().flatMap(
            entry -> entry.getValue().stream()
            .map(artifact -> artifactDirectory
                    .resolve(entry.getKey())
                    .resolve(findArtifactName(Path.of(artifact)))
                    .resolve(findArtifactVersion(Path.of(artifact)))
            )).toList();
        return new Plugin(plugin,artifactPaths);
    }


    private void checkArtifacts(PluginManifest plugin) {
        plugin.artifacts().forEach((group,artifacts)-> {
            artifacts.forEach(artifact -> checkArtifact(plugin, group, artifact));
        });
    }

    private void checkArtifact(PluginManifest plugin, String group, String artifact) {
        String file = artifact + ".jar";
        if (Files.notExists(artifactDirectory.resolve(group).resolve(file))) {
            throw new PluginException(
                "Cannot load plugin {} : artifact {}/{} not present; please reinstall the plugin",
                plugin.id(),
                    group,
                    artifact
            );
        }
    }




    private boolean validatePluginApplication(PluginManifest manifest) {
        if (!manifest.application().equals(this.application)) {
           log.warn(
                "Plugin {} not suitable to the application module '{}' but '{}'",
                manifest.id(),
                this.application,
                manifest.application()
           );
           return false;
        }
        return true;
    }

}