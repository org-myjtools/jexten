package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TestPluginManager {

    @TempDir
    Path tempDir;

    @Test
    public void installPluginFromBundleFile() {
        PluginManager pluginManager = new PluginManager("Plugin-Application",tempDir);
        pluginManager.installPluginFromBundle(Path.of("src/test/resources/plugin.zip"));
        assertThat(tempDir.resolve("artifacts/assertj/assertj-core-3.27.1.jar")).exists();
        assertThat(tempDir.resolve("artifacts/slf4j/slf4j-simple-2.0.16.jar")).exists();
        assertThat(tempDir.resolve("manifests/Plugin-Group-Plugin-Name.yaml")).exists();
    }


    @Test
    public void removePlugin() {
        PluginManager pluginManager = new PluginManager("Plugin-Application",tempDir);
        pluginManager.installPluginFromBundle(Path.of("src/test/resources/plugin.zip"));
        var pluginID = new PluginID("Plugin-Group", "Plugin-Name");
        pluginManager.removePlugin(pluginID);
        assertThat(pluginManager.getPlugin(pluginID)).isEmpty();
        assertThat(tempDir.resolve("manifests/Plugin-Group-Plugin-Name.yaml")).doesNotExist();
    }

    @Test
    public void installPluginFromJar() {
        PluginManager pluginManager = new PluginManager("Plugin-Application",tempDir);
        pluginManager.setArtifactStore(request-> Map.of(
            "assertj", List.of(Path.of("src/test/resources/mock_repo/assertj-core-3.27.1.jar")),
            "slf4j",   List.of(Path.of("src/test/resources/mock_repo/slf4j-simple-2.0.16.jar"))
        ));
        pluginManager.installPluginFromJar(Path.of("src/test/resources/plugin.jar"));
        assertThat(tempDir.resolve("artifacts/assertj/assertj-core-3.27.1.jar")).exists();
        assertThat(tempDir.resolve("artifacts/slf4j/slf4j-simple-2.0.16.jar")).exists();
        assertThat(tempDir.resolve("manifests/Plugin-Group-Plugin-Name.yaml")).exists();
    }
}
