package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.PluginException;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class TestPluginRuntimeDependencies {

    private static final Path BUNDLE = Path.of("src/test/resources/plugin.zip");
    private static final PluginID PLUGIN_ID = new PluginID("Plugin-Group", "Plugin-Name");

    @TempDir
    Path tempDir;

    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        pluginManager = new PluginManager("Plugin-Application", TestPluginRuntimeDependencies.class.getClassLoader(), tempDir);
        pluginManager.installPluginFromBundle(BUNDLE);
    }


    @Test
    void addRuntimeDependency_persistsRuntimeConfigFile() {
        pluginManager.setArtifactStore(request -> Map.of(
            "com.h2database", List.of(Path.of("src/test/resources/mock_repo/assertj-core-3.27.1.jar"))
        ));

        pluginManager.addRuntimeDependency(PLUGIN_ID, "com.h2database", "assertj-core-3.27.1");

        assertThat(tempDir.resolve("manifests/Plugin-Group-Plugin-Name.runtime.yaml")).exists();
    }


    @Test
    void addRuntimeDependency_artifactAlreadyPresent_doesNotRequireStore() {
        // assertj-core-3.27.1 is already in the artifact dir from the bundle install
        pluginManager.addRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");

        assertThat(pluginManager.getRuntimeDependencies(PLUGIN_ID))
            .containsEntry("assertj", List.of("assertj-core-3.27.1"));
    }


    @Test
    void addRuntimeDependency_appearsInGetRuntimeDependencies() {
        pluginManager.setArtifactStore(request -> Map.of(
            "com.h2database", List.of(Path.of("src/test/resources/mock_repo/assertj-core-3.27.1.jar"))
        ));

        pluginManager.addRuntimeDependency(PLUGIN_ID, "com.h2database", "assertj-core-3.27.1");

        Map<String, List<String>> deps = pluginManager.getRuntimeDependencies(PLUGIN_ID);
        assertThat(deps).containsKey("com.h2database");
        assertThat(deps.get("com.h2database")).contains("assertj-core-3.27.1");
    }


    @Test
    void addRuntimeDependency_multipleDeps_allPersisted() {
        pluginManager.setArtifactStore(request -> Map.of(
            "assertj", List.of(Path.of("src/test/resources/mock_repo/assertj-core-3.27.1.jar")),
            "slf4j",   List.of(Path.of("src/test/resources/mock_repo/slf4j-simple-2.0.16.jar"))
        ));

        pluginManager.addRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");
        pluginManager.addRuntimeDependency(PLUGIN_ID, "slf4j", "slf4j-simple-2.0.16");

        Map<String, List<String>> deps = pluginManager.getRuntimeDependencies(PLUGIN_ID);
        assertThat(deps).containsKeys("assertj", "slf4j");
    }


    @Test
    void removeRuntimeDependency_removesEntryAndReturnsTrue() {
        pluginManager.addRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");

        boolean removed = pluginManager.removeRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");

        assertThat(removed).isTrue();
        assertThat(pluginManager.getRuntimeDependencies(PLUGIN_ID)).doesNotContainKey("assertj");
    }


    @Test
    void removeRuntimeDependency_nonExistentEntry_returnsFalse() {
        boolean removed = pluginManager.removeRuntimeDependency(PLUGIN_ID, "com.example", "nonexistent-1.0.0");

        assertThat(removed).isFalse();
    }


    @Test
    void removeRuntimeDependency_lastEntry_deletesConfigFile() {
        pluginManager.addRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");
        Path runtimeConfig = tempDir.resolve("manifests/Plugin-Group-Plugin-Name.runtime.yaml");
        assertThat(runtimeConfig).exists();

        pluginManager.removeRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");

        assertThat(runtimeConfig).doesNotExist();
    }


    @Test
    void removePlugin_alsoCleansUpRuntimeConfigFile() {
        pluginManager.addRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");
        Path runtimeConfig = tempDir.resolve("manifests/Plugin-Group-Plugin-Name.runtime.yaml");
        assertThat(runtimeConfig).exists();

        pluginManager.removePlugin(PLUGIN_ID);

        assertThat(runtimeConfig).doesNotExist();
    }


    @Test
    void getRuntimeDependencies_pluginNotInstalled_throwsException() {
        PluginID unknown = new PluginID("unknown", "plugin");

        assertThatThrownBy(() -> pluginManager.getRuntimeDependencies(unknown))
            .isInstanceOf(PluginException.class)
            .hasMessageContaining("is not installed");
    }


    @Test
    void addRuntimeDependency_pluginNotInstalled_throwsException() {
        PluginID unknown = new PluginID("unknown", "plugin");

        assertThatThrownBy(() -> pluginManager.addRuntimeDependency(unknown, "com.example", "lib-1.0.0"))
            .isInstanceOf(PluginException.class)
            .hasMessageContaining("is not installed");
    }


    @Test
    void addRuntimeDependency_artifactMissingAndNoStore_throwsException() {
        assertThatThrownBy(() ->
            pluginManager.addRuntimeDependency(PLUGIN_ID, "com.missing", "nonexistent-1.0.0"))
            .isInstanceOf(PluginException.class)
            .hasMessageContaining("Artifact store is not set");
    }


    @Test
    void getRuntimeDependencies_noDepsConfigured_returnsEmptyMap() {
        Map<String, List<String>> deps = pluginManager.getRuntimeDependencies(PLUGIN_ID);

        assertThat(deps).isEmpty();
    }


    @Test
    void runtimeConfigSurvivestManagerRefresh() {
        pluginManager.addRuntimeDependency(PLUGIN_ID, "assertj", "assertj-core-3.27.1");

        pluginManager.refresh();

        assertThat(pluginManager.getRuntimeDependencies(PLUGIN_ID))
            .containsEntry("assertj", List.of("assertj-core-3.27.1"));
    }
}