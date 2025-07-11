package org.myjtools.jexten.plugin;

import java.nio.file.Path;

/**
 * Represents a plugin file in the form of a JAR file.
 * This class extends PluginFile and provides functionality to read a JAR file as a plugin.
 */
public final class PluginJarFile extends PluginFile {


   public static PluginJarFile read(Path path) {
        if (!path.toString().endsWith(".jar")) {
            throw new PluginException("Invalid format of plugin file {} , jar expected",path.getFileName());
        }
        var file = new PluginJarFile(path);
        file.read();
        return file;
   }

    private PluginJarFile(Path path) {
        super(path);
    }


}
