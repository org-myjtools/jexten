package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPlugin {


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
}
