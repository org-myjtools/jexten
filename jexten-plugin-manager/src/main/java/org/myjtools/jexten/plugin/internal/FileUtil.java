package org.myjtools.jexten.plugin.internal;

import org.myjtools.jexten.plugin.PluginException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {


    private FileUtil() {
        // Utility class, no instantiation allowed
    }

    public static void deleteFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new PluginException("Failed to delete file: %s", path, e);
        }
    }


    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetFolder) throws IOException {
        Path targetDirResolved = targetFolder.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetFolder)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizePath;
    }


    public static void unzipFile(ZipFile zipFile, ZipEntry zipEntry, Path newPath) throws IOException {
        if (newPath.getParent() != null && Files.notExists(newPath.getParent())) {
            Files.createDirectories(newPath.getParent());
        }
        Files.copy(zipFile.getInputStream(zipEntry), newPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String findArtifactName(Path artifact) {
        String filename = artifact.getFileName().toString().replace(".jar", "");
        int versionStart = findVersionBoundary(filename);
        if (versionStart == -1) {
            throw new IllegalArgumentException("invalid artifact filename: " + filename);
        }
        return filename.substring(0, versionStart - 1);
    }

    public static String findArtifactVersion(Path artifact) {
        String filename = artifact.getFileName().toString().replace(".jar", "");
        int versionStart = findVersionBoundary(filename);
        if (versionStart == -1) {
            throw new IllegalArgumentException("invalid artifact filename: " + filename);
        }
        return filename.substring(versionStart);
    }

    /**
     * Finds the index where the version part starts in an artifact filename.
     * The version is identified as the first segment starting with a digit
     * (i.e., the first hyphen followed by a digit).
     * Returns the index of the first digit of the version, or -1 if not found.
     */
    public static int findVersionBoundary(String filename) {
        for (int i = 0; i < filename.length() - 1; i++) {
            if (filename.charAt(i) == '-' && Character.isDigit(filename.charAt(i + 1))) {
                return i + 1;
            }
        }
        return -1;
    }

    public static Yaml yamlWriter() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(options) {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    return null;
                }
                if (propertyValue instanceof Map && ((Map<?, ?>) propertyValue).isEmpty()) {
                    return null;
                }
                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
            }
        };
        representer.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
        return new Yaml(representer, options);
    }
}
