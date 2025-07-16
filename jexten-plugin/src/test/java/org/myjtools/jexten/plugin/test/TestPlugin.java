package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.jexten.plugin.Plugin;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestPlugin {

    @Test
    void testPlugin() throws IOException {
        PluginManifest manifest = PluginManifest.read(Files.newBufferedReader(Path.of("src/test/resources/plugin.yaml")));
        Plugin plugin = new Plugin(manifest, List.of(Path.of("src/test/resources/mock_repo")));
        assertThat(plugin.moduleReferences()).hasSize(2);
    }
}
