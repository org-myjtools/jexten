package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.jexten.plugin.ModuleLayerTree;
import org.myjtools.jexten.plugin.ModuleLayerTreeVisitor;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("ModuleLayerTree Tests")
public class TestModuleLayerTree {


    @Nested
    @DisplayName("Boot Layer Tree Tests")
    class BootLayerTreeTests {

        private ModuleLayerTree bootTree;

        @BeforeEach
        void setUp() {
            // Create a tree with only the boot layer (no plugins)
            bootTree = new ModuleLayerTree(Map.of());
        }

        @Test
        @DisplayName("should have boot layer as root")
        void shouldHaveBootLayerAsRoot() {
            assertThat(bootTree.moduleLayer()).isEqualTo(ModuleLayer.boot());
        }

        @Test
        @DisplayName("should have no parent for boot layer")
        void shouldHaveNoParentForBootLayer() {
            assertThat(bootTree.parent()).isEmpty();
        }

        @Test
        @DisplayName("should have no plugin for boot layer")
        void shouldHaveNoPluginForBootLayer() {
            assertThat(bootTree.plugin()).isEmpty();
        }

        @Test
        @DisplayName("should have depth 0 for boot layer")
        void shouldHaveDepthZeroForBootLayer() {
            assertThat(bootTree.depth()).isEqualTo(0);
        }

        @Test
        @DisplayName("should have modules (excluding java.* modules)")
        void shouldHaveModulesExcludingJavaModules() {
            List<Module> modules = bootTree.modules();
            // Should not contain java.*, jdk.*, javax.* modules
            assertThat(modules).noneMatch(m -> m.getName().startsWith("java."));
            assertThat(modules).noneMatch(m -> m.getName().startsWith("jdk."));
            assertThat(modules).noneMatch(m -> m.getName().startsWith("javax."));
        }

        @Test
        @DisplayName("should return sorted modules list")
        void shouldReturnSortedModulesList() {
            List<Module> modules = bootTree.modules();
            List<String> names = modules.stream().map(Module::getName).toList();
            List<String> sortedNames = new ArrayList<>(names);
            sortedNames.sort(String::compareTo);
            assertThat(names).isEqualTo(sortedNames);
        }
    }


    @Nested
    @DisplayName("Stream Tests")
    class StreamTests {

        @Test
        @DisplayName("should stream boot layer only when no plugins")
        void shouldStreamBootLayerOnlyWhenNoPlugins() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());

            List<ModuleLayerTree> nodes = tree.stream().toList();

            assertThat(nodes).hasSize(1);
            assertThat(nodes.get(0).depth()).isEqualTo(0);
        }

        @Test
        @DisplayName("stream should start with root node")
        void streamShouldStartWithRootNode() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());

            ModuleLayerTree first = tree.stream().findFirst().orElseThrow();

            assertThat(first).isSameAs(tree);
        }

        @Test
        @DisplayName("stream should include all nodes")
        void streamShouldIncludeAllNodes() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());

            long count = tree.stream().count();

            assertThat(count).isGreaterThanOrEqualTo(1);
        }
    }


    @Nested
    @DisplayName("forEach with Consumer Tests")
    class ForEachConsumerTests {

        @Test
        @DisplayName("should visit boot layer")
        void shouldVisitBootLayer() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());
            List<ModuleLayerTree> visited = new ArrayList<>();

            tree.forEach(visited::add);

            assertThat(visited).hasSize(1);
            assertThat(visited.get(0).depth()).isEqualTo(0);
        }

        @Test
        @DisplayName("should visit all nodes in tree order")
        void shouldVisitAllNodesInTreeOrder() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());
            List<Integer> depths = new ArrayList<>();

            tree.forEach(node -> depths.add(node.depth()));

            // First node should always be depth 0 (root)
            assertThat(depths.get(0)).isEqualTo(0);
        }
    }


    @Nested
    @DisplayName("forEach with Visitor Tests")
    class ForEachVisitorTests {

        @Test
        @DisplayName("should call enterLayer and exitLayer for boot layer")
        void shouldCallEnterAndExitForBootLayer() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());
            List<String> calls = new ArrayList<>();

            tree.forEach(new ModuleLayerTreeVisitor() {
                @Override
                public void enterLayer(ModuleLayer layer, PluginManifest plugin, int depth) {
                    calls.add("enter:" + depth);
                }

                @Override
                public void exitLayer(ModuleLayer layer, PluginManifest plugin, int depth) {
                    calls.add("exit:" + depth);
                }

                @Override
                public void visitModule(ModuleLayer layer, PluginManifest plugin, int depth, Module module) {
                    // Not tracking modules for this test
                }
            });

            assertThat(calls).contains("enter:0", "exit:0");
            // Enter should come before exit
            assertThat(calls.indexOf("enter:0")).isLessThan(calls.indexOf("exit:0"));
        }

        @Test
        @DisplayName("should call visitModule for each module")
        void shouldCallVisitModuleForEachModule() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());
            AtomicInteger moduleCount = new AtomicInteger(0);

            tree.forEach(new ModuleLayerTreeVisitor() {
                @Override
                public void enterLayer(ModuleLayer layer, PluginManifest plugin, int depth) {}

                @Override
                public void exitLayer(ModuleLayer layer, PluginManifest plugin, int depth) {}

                @Override
                public void visitModule(ModuleLayer layer, PluginManifest plugin, int depth, Module module) {
                    moduleCount.incrementAndGet();
                }
            });

            assertThat(moduleCount.get()).isEqualTo(tree.modules().size());
        }

        @Test
        @DisplayName("visitor should receive null plugin for boot layer")
        void visitorShouldReceiveNullPluginForBootLayer() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());
            List<PluginManifest> plugins = new ArrayList<>();

            tree.forEach(new ModuleLayerTreeVisitor() {
                @Override
                public void enterLayer(ModuleLayer layer, PluginManifest plugin, int depth) {
                    plugins.add(plugin);
                }

                @Override
                public void exitLayer(ModuleLayer layer, PluginManifest plugin, int depth) {}

                @Override
                public void visitModule(ModuleLayer layer, PluginManifest plugin, int depth, Module module) {}
            });

            assertThat(plugins).containsNull();
        }
    }


    @Nested
    @DisplayName("Description Tests")
    class DescriptionTests {

        @Test
        @DisplayName("should generate description for boot layer")
        void shouldGenerateDescriptionForBootLayer() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());

            String description = tree.description();

            assertThat(description).isNotNull();
            assertThat(description).isNotEmpty();
            assertThat(description).contains("ApplicationLayer");
        }

        @Test
        @DisplayName("description should contain module names")
        void descriptionShouldContainModuleNames() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());

            String description = tree.description();

            // Should contain at least some module references
            assertThat(description).contains("-");
        }

        @Test
        @DisplayName("description should have proper formatting")
        void descriptionShouldHaveProperFormatting() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());

            String description = tree.description();

            // Should have multiple lines
            assertThat(description.split("\n").length).isGreaterThan(1);
            // Should start with application layer header
            assertThat(description.trim()).startsWith("[");
        }
    }


    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("modules list should be immutable")
        void modulesListShouldBeImmutable() {
            ModuleLayerTree tree = new ModuleLayerTree(Map.of());
            List<Module> modules = tree.modules();

            // The list should be unmodifiable
            assertThat(modules).isUnmodifiable();
        }
    }
}