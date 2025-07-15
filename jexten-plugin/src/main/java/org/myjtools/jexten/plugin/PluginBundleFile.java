package org.myjtools.jexten.plugin;

import org.myjtools.jexten.plugin.internal.FileUtil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;


public final class PluginBundleFile extends PluginFile {


    /**
     * Reads a plugin bundle from the specified path.
     *
     * @param path the path to the plugin bundle (should be a .zip file)
     * @return a PluginBundle instance containing the plugin manifest
     * @throws PluginException if the file format is invalid or if an error occurs while reading
     */
    public static PluginBundleFile read(Path path) {
        if (!path.toString().endsWith(".zip")) {
            throw new PluginException("Invalid format of plugin file {} , zip expected",path.getFileName());
        }
        var file = new PluginBundleFile(path);
        file.read();
        Objects.requireNonNull(file.plugin);
        return file;
    }

    /**
     * Creates a new plugin bundle at the specified path with the given plugin manifest and dependencies.
     *
     * @param plugin       the plugin manifest to include in the bundle
     * @param path         the path where the plugin bundle will be created (should be a .zip file)
     * @param dependencies a list of paths to dependency files to include in the bundle
     * @return a PluginBundle instance containing the created plugin manifest
     * @throws PluginException if an error occurs while creating the bundle
     */
    public static PluginBundleFile assemble(PluginManifest plugin, Path path, List<Path> dependencies) {
        return new PluginBundleFile(path).assemble(plugin,dependencies);
    }


    private PluginBundleFile(Path path) {
        super(path);
    }


    private PluginBundleFile assemble(PluginManifest plugin, List<Path> dependencies) {
        this.plugin = plugin;
        try (var output = new ZipOutputStream(new FileOutputStream(path.toFile()))) {
            output.putNextEntry(new ZipEntry(PLUGIN_MANIFEST_FILE));
            plugin.write(new OutputStreamWriter(output));
            for (Path dependency : dependencies) {
                output.putNextEntry(new ZipEntry(dependency.toFile().getName()));
                Files.copy(dependency, output);
            }
            return this;
        } catch (IOException | RuntimeException e) {
            throw PluginException.wrapper(e);
        }
    }


    /**
     * Unzips the plugin bundle into the specified target folder.
     * The target folder will be created if it does not exist.
     *
     * @param targetFolder the folder where the plugin files will be extracted
     * @throws IOException if an error occurs while unzipping
     */
    public void extract(Path targetFolder) throws IOException {
        try (var zipFile = new ZipFile(path.toFile())) {
            zipFile.stream().forEach(zipEntry -> extract(zipFile, zipEntry, targetFolder));
        }
    }


    private void extract(ZipFile zipFile, ZipEntry zipEntry, Path targetFolder) {
        try {
            Path newPath = FileUtil.zipSlipProtect(zipEntry, targetFolder);
            if (newPath.toString().endsWith(".jar")) {
                String group = findArtifactGroup(newPath);
                newPath = newPath.getParent().resolve(group).resolve(newPath.getFileName());
                if (Files.exists(newPath)) {
                    return;
                }
                FileUtil.unzipFile(zipFile, zipEntry, newPath);
            }
        } catch (IOException e) {
            throw PluginException.wrapper(e);
        }
    }

    private String findArtifactGroup(Path artifact) {
        String artifactName = artifact.getFileName().toString().replace(".jar","");
        return plugin.artifacts().entrySet().stream()
            .filter(e -> e.getValue().contains(artifactName))
            .findFirst()
            .map(Map.Entry::getKey)
            .orElse(null);
    }


}
