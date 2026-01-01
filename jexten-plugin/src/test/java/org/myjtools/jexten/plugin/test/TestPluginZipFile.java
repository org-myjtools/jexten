package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.internal.PluginBundleFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TestPluginZipFile {

    @TempDir
    Path tempDir;

    @Test
    void unzipPluginFile() throws IOException {
        Path path = Path.of("src/test/resources/plugin.zip");
        PluginBundleFile file = PluginBundleFile.read(path);
        file.extract(tempDir);
        assertThat(tempDir.resolve("assertj/assertj-core/3.27.1/assertj-core-3.27.1.jar")).exists();
        assertThat(tempDir.resolve("slf4j/slf4j-simple/2.0.16/slf4j-simple-2.0.16.jar")).exists();
    }


    @Test
    void readMalformedPluginFile() {
        assertThatCode( () -> {
            Path path = Path.of("src/test/resources/malformed-plugin.zip");
            PluginBundleFile.read(path);
        }).hasMessage("Plugin manifest not present");
    }

}
