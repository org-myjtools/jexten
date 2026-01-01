package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.jexten.plugin.InvalidManifestException;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestPluginManifest {


    @Test
    void readAndWritePluginFile() throws IOException {
        String content = """
            application: Plugin Application
            artifacts:
              group1:
              - plugin-a-1.2
              - lib-b-2.9
              group2:
              - lib-c-1.3
            description: Plugin description
            displayName: Plugin Display name
            extensionPoints:
            - package.extensionPointA
            extensions:
              package.extension.point:
              - package.extension1
            group: Plugin Group
            hostModule: Plugin.Host.Module
            licenseName: MIT
            licenseText: |
              Copyright <YEAR> <COPYRIGHT HOLDER> Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
            name: Plugin Name
            url: http://plugin.com
            version: '3.12'
            """;
        PluginManifest plugin = PluginManifest.read(new StringReader(content));
        StringWriter writer = new StringWriter();
        plugin.write(writer);
        assertThat(writer.toString()).isEqualTo(content);
    }

    @Test
    void createPluginUsingBuilder() {
        PluginManifest plugin = PluginManifest.builder()
                .group("mygroup")
                .name("name")
                .version("1.0.0")
                .application("myapp")
                .displayName("My Plugin")
                .description("This is a test plugin")
                .url("http://example.com/plugin")
                .licenseName("MIT")
                .licenseText("Copyright (c) 2023 My Company")
                .build();
        assertThat(plugin.application()).isEqualTo("myapp");
        assertThat(plugin.name()).isEqualTo("name");
    }


    @Nested
    class ManifestValidation {

        @Test
        void shouldRejectEmptyManifest() {
            String content = "{}";
            assertThatThrownBy(() -> PluginManifest.read(new StringReader(content)))
                    .isInstanceOf(InvalidManifestException.class)
                    .satisfies(ex -> {
                        var errors = ((InvalidManifestException) ex).validationErrors();
                        assertThat(errors).contains("'group' is required");
                        assertThat(errors).contains("'name' is required");
                        assertThat(errors).contains("'version' is required");
                        assertThat(errors).contains("'hostModule' is required");
                    });
        }


        @Test
        void shouldRejectMissingGroup() {
            String content = """
                name: my-plugin
                version: 1.0.0
                hostModule: my.module
                """;
            assertThatThrownBy(() -> PluginManifest.read(new StringReader(content)))
                    .isInstanceOf(InvalidManifestException.class)
                    .satisfies(ex -> {
                        var errors = ((InvalidManifestException) ex).validationErrors();
                        assertThat(errors).containsExactly("'group' is required");
                    });
        }


        @Test
        void shouldRejectInvalidVersionFormat() {
            String content = """
                group: my.group
                name: my-plugin
                version: invalid-version
                hostModule: my.module
                """;
            assertThatThrownBy(() -> PluginManifest.read(new StringReader(content)))
                    .isInstanceOf(InvalidManifestException.class)
                    .satisfies(ex -> {
                        var errors = ((InvalidManifestException) ex).validationErrors();
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0)).contains("'version' must be a valid semantic version");
                    });
        }


        @Test
        void shouldAcceptValidSemanticVersions() throws IOException {
            String[] validVersions = {"1.0", "1.0.0", "2.3.4", "1.0.0-SNAPSHOT"};
            for (String version : validVersions) {
                String content = """
                    group: my.group
                    name: my-plugin
                    version: '%s'
                    hostModule: my.module
                    """.formatted(version);
                PluginManifest manifest = PluginManifest.read(new StringReader(content));
                assertThat(manifest.version().toString()).isNotNull();
            }
        }


        @Test
        void shouldRejectEmptyArtifactDependencies() {
            String content = """
                group: my.group
                name: my-plugin
                version: 1.0.0
                hostModule: my.module
                artifacts:
                  empty-artifact: []
                """;
            assertThatThrownBy(() -> PluginManifest.read(new StringReader(content)))
                    .isInstanceOf(InvalidManifestException.class)
                    .satisfies(ex -> {
                        var errors = ((InvalidManifestException) ex).validationErrors();
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0)).contains("'empty-artifact' must have at least one dependency");
                    });
        }


        @Test
        void shouldRejectEmptyExtensionImplementations() {
            String content = """
                group: my.group
                name: my-plugin
                version: 1.0.0
                hostModule: my.module
                extensions:
                  my.extension.Point: []
                """;
            assertThatThrownBy(() -> PluginManifest.read(new StringReader(content)))
                    .isInstanceOf(InvalidManifestException.class)
                    .satisfies(ex -> {
                        var errors = ((InvalidManifestException) ex).validationErrors();
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0)).contains("'my.extension.Point' must have at least one implementation");
                    });
        }


        @Test
        void shouldCollectAllValidationErrors() {
            String content = """
                version: not-valid
                artifacts:
                  empty: []
                extensions:
                  empty.point: []
                """;
            assertThatThrownBy(() -> PluginManifest.read(new StringReader(content)))
                    .isInstanceOf(InvalidManifestException.class)
                    .satisfies(ex -> {
                        var errors = ((InvalidManifestException) ex).validationErrors();
                        assertThat(errors).hasSizeGreaterThanOrEqualTo(5);
                    });
        }


        @Test
        void shouldAcceptValidManifest() {
            String content = """
                group: my.group
                name: my-plugin
                version: 1.0.0
                hostModule: my.module
                artifacts:
                  main:
                    - com.example:lib:1.0
                extensions:
                  my.extension.Point:
                    - my.extension.Impl
                """;
            PluginManifest manifest = PluginManifest.read(new StringReader(content));
            assertThat(manifest.group()).isEqualTo("my.group");
            assertThat(manifest.name()).isEqualTo("my-plugin");
        }
    }
}
