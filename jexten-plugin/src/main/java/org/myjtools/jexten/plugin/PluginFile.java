package org.myjtools.jexten.plugin;

import org.myjtools.jexten.plugin.internal.PluginBundleFile;
import org.myjtools.jexten.plugin.internal.PluginJarFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents a plugin file, which can be a JAR or ZIP file containing a plugin manifest.
 * This class provides methods to read the plugin manifest and retrieve plugin information.
 */
public sealed abstract class PluginFile permits PluginBundleFile, PluginJarFile {


    public static final String PLUGIN_MANIFEST_FILE = "plugin.yaml";

    protected final Path path;
    protected PluginManifest plugin;


    protected PluginFile(Path path) {
        this.path = path;
    }


    protected void read() {
        this.plugin = locatePluginManifest(path);
    }


    /**
     * @return The ID of the plugin represented by this file.
     */
    public PluginID pluginID() {
        return plugin.id();
    }


    /**
     * @return The plugin manifest associated with this file.
     */
    public PluginManifest plugin() {
        return Objects.requireNonNull(plugin);
    }

    /**
     * @return The path to the plugin file.
     */
    public Path path() {
        return path;
    }


    protected static PluginManifest locatePluginManifest(Path path) {
        try (ZipFile file = new ZipFile(path.toFile())) {

            var pluginManifestZipEntry = file
                .stream()
                .filter(zipEntry -> !zipEntry.isDirectory())
                .filter(PluginFile::isPluginManifestEntry)
                .findFirst()
                .orElseThrow(() -> new PluginException("Plugin manifest not present"));

            try (var entryInputStream = file.getInputStream(pluginManifestZipEntry)) {
                return PluginManifest.read(new InputStreamReader(entryInputStream));
            }

        } catch (IOException | RuntimeException e) {
            throw PluginException.wrapper(e);
        }
    }


    private static boolean isPluginManifestEntry(ZipEntry zipEntry) {
        return new File(zipEntry.getName()).getName().equals(PLUGIN_MANIFEST_FILE);
    }


}
