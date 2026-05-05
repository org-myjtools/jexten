package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.ModuleLayerTree;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;
import org.myjtools.jexten.plugin.PluginManifest;
import org.myjtools.jexten.plugin.internal.Plugin;
import org.myjtools.jexten.plugin.internal.PluginMap;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;

class TestPluginCoverage {

    @TempDir
    Path tempDir;


    @Test
    void buildModuleLayerUsesParentLayerModulesWithoutDuplicatingThem() throws IOException {
        Path parentJar = createAutomaticModuleJar("simple-plugin-1.0.jar");
        Path childJar = createAutomaticModuleJar("extra-plugin-1.0.jar");

        Plugin parentPlugin = new Plugin(
            manifest("test.group", "parent-plugin", "java.base"),
            List.of(parentJar)
        );
        ModuleLayer parentLayer = parentPlugin
            .buildModuleLayer(ModuleLayer.boot(), getClass().getClassLoader())
            .orElseThrow();

        Plugin childPlugin = new Plugin(
            manifest("test.group", "child-plugin", "simple.plugin"),
            List.of(parentJar, childJar)
        );

        assertThat(childPlugin.isHostedBy(parentLayer)).isTrue();
        assertThat(childPlugin.moduleNames(parentLayer)).containsExactly("extra.plugin");

        Optional<ModuleLayer> childLayer = childPlugin.buildModuleLayer(parentLayer, getClass().getClassLoader());

        assertThat(childLayer).isPresent();
        assertThat(childLayer.orElseThrow().modules())
            .extracting(Module::getName)
            .containsExactly("extra.plugin");
        assertThat(childPlugin.toString()).isEqualTo("test.group:child-plugin");
    }


    @Test
    void buildModuleLayerReturnsEmptyWhenPluginDependenciesCannotBeResolved() throws IOException {
        Path helperJar = createAutomaticModuleJar("helper-module-1.0.jar");
        Path brokenJar = createModuleJar(
            "broken-plugin-1.0.jar",
            "broken.plugin",
            List.of(helperJar),
            "helper.module"
        );

        Plugin plugin = new Plugin(
            manifest("test.group", "broken-plugin", "java.base"),
            List.of(brokenJar)
        );

        assertThat(plugin.isHostedBy(ModuleLayer.boot())).isTrue();
        assertThat(plugin.moduleNames(ModuleLayer.boot())).containsExactly("broken.plugin");
        assertThat(plugin.buildModuleLayer(ModuleLayer.boot(), getClass().getClassLoader())).isEmpty();
    }


    @Test
    void pluginWithoutLoadableArtifactsHasNoModules() throws IOException {
        Path emptyDirectory = Files.createDirectory(tempDir.resolve("empty-artifacts"));

        Plugin plugin = new Plugin(
            manifest("test.group", "empty-plugin", "java.base"),
            List.of(emptyDirectory)
        );

        assertThat(plugin.moduleReferences()).isEmpty();
        assertThat(plugin.moduleNames(ModuleLayer.boot())).isEmpty();
    }


    @Test
    void pluginMapBuildsLayersAndCachesTheTree() throws IOException {
        Path pluginJar = createAutomaticModuleJar("map-plugin-1.0.jar");
        PluginManifest manifest = manifest("map.group", "map-plugin", "java.base");
        Plugin plugin = new Plugin(manifest, List.of(pluginJar));
        PluginMap pluginMap = new PluginMap(getClass().getClassLoader());

        pluginMap.add(plugin);

        assertThat(pluginMap.get(manifest.id())).contains(plugin);
        assertThat(pluginMap.getVersion(manifest.id())).contains(manifest.version());
        assertThat(pluginMap.containsKey(manifest.id())).isTrue();
        assertThat(pluginMap.ids()).containsExactly(manifest.id());

        List<ModuleLayer> firstLayers = pluginMap.layers().toList();
        List<ModuleLayer> secondLayers = pluginMap.layers().toList();
        ModuleLayerTree tree = pluginMap.moduleLayerTree();

        assertThat(firstLayers).hasSize(1);
        assertThat(firstLayers.getFirst().modules())
            .extracting(Module::getName)
            .containsExactly("map.plugin");
        assertThat(secondLayers).containsExactlyElementsOf(firstLayers);
        assertThat(tree.stream())
            .extracting(node -> node.plugin().map(PluginManifest::id).orElse(null))
            .contains((PluginID) null, manifest.id());
    }


    @Test
    void pluginManagerExposesModuleLayerTree() {
        PluginManager pluginManager = new PluginManager(
            "Plugin-Application",
            getClass().getClassLoader(),
            tempDir
        );

        pluginManager.installPluginFromBundle(Path.of("src/test/resources/plugin.zip"));
        ModuleLayerTree tree = pluginManager.moduleLayerTree();

        assertThat(tree.moduleLayer()).isEqualTo(ModuleLayer.boot());
        assertThat(tree.depth()).isZero();
    }


    private PluginManifest manifest(String group, String name, String hostModule) {
        return PluginManifest.read(new StringReader("""
            group: %s
            name: %s
            version: 1.0.0
            hostModule: %s
            """.formatted(group, name, hostModule)));
    }


    private Path createAutomaticModuleJar(String fileName) throws IOException {
        Path jarPath = tempDir.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("marker.txt"));
            jar.write("ok".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarPath;
    }


    private Path createModuleJar(
        String fileName,
        String moduleName,
        List<Path> modulePath,
        String... requiredModules
    ) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();

        Path sourceDir = tempDir.resolve(moduleName + "-src");
        Path classesDir = tempDir.resolve(moduleName + "-classes");
        Files.createDirectories(sourceDir);
        Files.createDirectories(classesDir);

        String requires = java.util.Arrays.stream(requiredModules)
            .map(module -> "    requires " + module + ";\n")
            .collect(Collectors.joining());
        Path sourceFile = sourceDir.resolve("module-info.java");
        Files.writeString(sourceFile, "module " + moduleName + " {\n" + requires + "}\n");

        List<String> compilerArgs = new ArrayList<>();
        if (!modulePath.isEmpty()) {
            compilerArgs.add("--module-path");
            compilerArgs.add(modulePath.stream()
                .map(Path::toString)
                .collect(Collectors.joining(System.getProperty("path.separator"))));
        }
        compilerArgs.add("-d");
        compilerArgs.add(classesDir.toString());
        compilerArgs.add(sourceFile.toString());

        assertThat(compiler.run(null, null, null, compilerArgs.toArray(String[]::new))).isZero();

        Path jarPath = tempDir.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("module-info.class"));
            jar.write(Files.readAllBytes(classesDir.resolve("module-info.class")));
            jar.closeEntry();
        }
        return jarPath;
    }
}
