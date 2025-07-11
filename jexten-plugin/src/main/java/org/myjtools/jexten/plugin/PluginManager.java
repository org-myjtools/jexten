package org.myjtools.jexten.plugin;

import org.myjtools.jexten.ModuleLayerProvider;
import org.myjtools.jexten.Version;
import org.myjtools.jexten.plugin.internal.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class PluginManager implements ModuleLayerProvider {


    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final String application;
    private final Path artifactDirectory;
    private final Path manifestDirectory;
    private final Map<PluginID, PluginModuleLayer> plugins = new HashMap<>();

    private ArtifactStore artifactStore;

    public PluginManager(String application, Path pluginDirectory) {
        try {
            log.info("Preparing PluginManager for {} using directory {}",application,pluginDirectory);
            this.application = application;
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
                checkArtifacts(plugin);
                plugins.put(plugin.id(),pluginModuleLayer(plugin));
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
        plugins.clear();
        discoverInstalledPlugins();
    }


    @Override
    public Stream<ModuleLayer> moduleLayers() {
        return Stream.empty(); // TODO
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
            installPluginManifest(manifest,manifestFile);
            bundle.extract(artifactDirectory);
        } catch (IOException  e) {
            throw new PluginException(e, "Cannot install plugin from file {}", bundleFile);
        }
    }


    /**
     * Install a plugin bundle from a URI. The bundle file must contain a valid plugin manifest.
     * @param pluginURI The URI to the plugin bundle file
     */
    public void installPluginFromBundle(URI pluginURI) {
        Path temporaryFile = null;
        try {
            temporaryFile = FileUtil.downloadZipFromUri(pluginURI);
            installPluginFromBundle(temporaryFile);
        } catch (IOException | InterruptedException e) {
            throw new PluginException(e, "Cannot install plugin from URI {}", pluginURI);
        } finally {
            FileUtil.deleteFile(temporaryFile);
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

        if (plugins.containsKey(pluginID)) {
            Version existingVersion = plugins.get(pluginID).plugin().version();
            if (existingVersion.compareTo(candidateVersion) >= 0) {
                log.warn("Plugin {} already present with version {}", pluginID, existingVersion);
            } else {
                log.info("Updating plugin {} from version {} to {}", pluginID, existingVersion, candidateVersion);
                Files.deleteIfExists(manifestFile);
                plugins.remove(pluginID);
            }
        } else {
            log.info("Installing plugin {} version {}", pluginID, candidateVersion);
        }

        try (Writer writer = Files.newBufferedWriter(manifestFile)) {
            manifest.write(writer);
        }
        plugins.put(pluginID, pluginModuleLayer(manifest));
    }


    private void installArtifact(String group, Path jarFile) {
        Path groupDirectory = artifactDirectory.resolve(group);
        try {
            Files.createDirectories(groupDirectory);
            Path targetFile = groupDirectory.resolve(jarFile.getFileName());
            Files.copy(jarFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Installed artifact {} in {}", jarFile.getFileName(), groupDirectory);
        } catch (IOException e) {
            throw new PluginException(e, "Cannot install artifact {} in {}", jarFile, groupDirectory);
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
        if (!plugins.containsKey(pluginID)) {
            throw new PluginException("Plugin {} is not present", pluginID);
        }
        try {
            plugins.remove(pluginID);
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




    public Optional<PluginManifest> getPlugin(PluginID pluginID) {
        return Optional.ofNullable(plugins.get(pluginID)).map(PluginModuleLayer::plugin);
    }


    private Path manifestPath(PluginID pluginID) {
        return manifestDirectory.resolve(pluginID.group() + "-" + pluginID.name() + ".yaml");
    }



    private PluginModuleLayer pluginModuleLayer(PluginManifest plugin) {
        List<Path> artifactPaths = plugin.artifacts().entrySet().stream().flatMap(
            entry -> entry.getValue().stream()
            .map(artifact -> artifactDirectory.resolve(entry.getKey()).resolve(artifact + ".jar"))
        ).toList();
        return new PluginModuleLayer(plugin,artifactPaths);
    }


    private void checkArtifacts(PluginManifest plugin) {
        plugin.artifacts().forEach((group,artifacts)->{
            artifacts.forEach(artifact -> {
                String file = artifact + ".jar";
                if (Files.notExists(artifactDirectory.resolve(group).resolve(file))) {
                    throw new PluginException(
                        "Cannot load plugin {} : artifact {}/{} not present; please reinstall the plugin",
                        plugin.id(),
                        group,
                        artifact
                    );
                }
            });
        });
    }


}