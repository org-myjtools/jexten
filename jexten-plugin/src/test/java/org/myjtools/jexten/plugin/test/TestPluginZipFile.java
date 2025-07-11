package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.PluginBundleFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TestPluginZipFile {

    @TempDir
    Path tempDir;

    @Test
    void unzipPluginFile() throws IOException {
        Path path = Path.of("src/test/resources/plugin.zip");
        PluginBundleFile file = PluginBundleFile.read(path);
        file.extract(tempDir);
        assertThat(tempDir.resolve("group1/plugin-a-1.2.jar")).exists();
        assertThat(tempDir.resolve("group1/lib-b-2.9.jar")).exists();
        assertThat(tempDir.resolve("group2/lib-c-1.3.jar")).exists();
    }


    @Test
    void readMalformedPluginFile() throws IOException {
        assertThatCode( () -> {
            Path path = Path.of("src/test/resources/malformed-plugin.zip");
            PluginBundleFile file = PluginBundleFile.read(path);
        }).hasMessage("Plugin manifest not present");
    }

}
