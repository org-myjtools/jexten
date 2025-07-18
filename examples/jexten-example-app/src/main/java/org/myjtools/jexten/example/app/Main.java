package org.myjtools.jexten.example.app;


import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {

        Path folder = Files.createTempDirectory("jexten-example-app-plugins");
        PluginManager pluginManager = getPluginManager(folder);

        ExtensionManager extensionManager = ExtensionManager.create(pluginManager);
        System.out.println("PLUGINS LOADED: " + pluginManager.plugins());

        System.out.println("-------------------------------\n");
        extensionManager.getExtensions(Greeter.class).forEach(greeter -> greeter.greet("John Doe"));

        // Remove a plugin and see the effect
        System.out.println("-------------------------------\n");

        pluginManager.removePlugin(new PluginID("org.myjtools.jexten.example","jexten-example-plugin-b"));
        extensionManager.getExtensions(Greeter.class).forEach(greeter -> greeter.greet("John Doe"));


    }

    private static PluginManager getPluginManager(Path folder) {
        Path pluginPath = Path.of("jexten-example-app/src/main/resources").toAbsolutePath();
        List<Path> plugins = List.of(
            Path.of("jexten-example-plugin-a-bundle-1.0.0.zip")
            ,Path.of("jexten-example-plugin-b-bundle-1.0.0.zip")
            ,Path.of("jexten-example-plugin-c-bundle-1.0.0.zip")
            ,Path.of("jexten-example-plugin-c1-bundle-1.0.0.zip")
            ,Path.of("jexten-example-plugin-c2-bundle-1.0.0.zip")
        );
        PluginManager pluginManager = new PluginManager(
            "org.myjtools.jexten.example.app",
            Main.class.getClassLoader(),
            folder
        );
        plugins.forEach(plugin -> pluginManager.installPluginFromBundle(pluginPath.resolve(plugin)));
        return pluginManager;
    }

}