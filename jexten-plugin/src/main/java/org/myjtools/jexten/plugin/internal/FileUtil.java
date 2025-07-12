package org.myjtools.jexten.plugin.internal;

import org.myjtools.jexten.plugin.PluginException;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger("org.myjtools.jexten.plugin");


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

}
