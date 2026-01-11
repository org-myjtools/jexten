package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TestPluginHotReload {


    private PluginManifest createManifest() {
        return PluginManifest.read(new StringReader("""
            group: com.example
            name: test-plugin
            version: 1.0.0
            hostModule: com.example.test
            """));
    }


    @Nested
    class PluginEventTests {

        @Test
        void installedEventShouldHaveCorrectType() {
            PluginManifest manifest = createManifest();
            PluginEvent event = PluginEvent.installed(manifest);

            assertThat(event.type()).isEqualTo(PluginEvent.Type.INSTALLED);
            assertThat(event.pluginId()).isEqualTo(manifest.id());
            assertThat(event.manifest()).isEqualTo(manifest);
        }

        @Test
        void unloadedEventShouldHaveCorrectType() {
            PluginManifest manifest = createManifest();
            PluginEvent event = PluginEvent.unloaded(manifest);

            assertThat(event.type()).isEqualTo(PluginEvent.Type.UNLOADED);
            assertThat(event.pluginId()).isEqualTo(manifest.id());
        }

        @Test
        void reloadedEventShouldHaveCorrectType() {
            PluginManifest manifest = createManifest();
            PluginEvent event = PluginEvent.reloaded(manifest);

            assertThat(event.type()).isEqualTo(PluginEvent.Type.RELOADED);
            assertThat(event.pluginId()).isEqualTo(manifest.id());
        }

        @Test
        void removedEventShouldHaveCorrectType() {
            PluginManifest manifest = createManifest();
            PluginEvent event = PluginEvent.removed(manifest);

            assertThat(event.type()).isEqualTo(PluginEvent.Type.REMOVED);
            assertThat(event.pluginId()).isEqualTo(manifest.id());
        }
    }


    @Nested
    class PluginListenerTests {

        @Test
        void listenerShouldReceiveEvents() {
            List<PluginEvent> receivedEvents = new ArrayList<>();
            PluginListener listener = receivedEvents::add;

            PluginManifest manifest = createManifest();
            listener.onPluginEvent(PluginEvent.installed(manifest));
            listener.onPluginEvent(PluginEvent.reloaded(manifest));

            assertThat(receivedEvents).hasSize(2);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.INSTALLED);
            assertThat(receivedEvents.get(1).type()).isEqualTo(PluginEvent.Type.RELOADED);
        }

        @Test
        void forTypesShouldFilterEvents() {
            List<PluginEvent> receivedEvents = new ArrayList<>();
            PluginListener listener = PluginListener.forTypes(
                    receivedEvents::add,
                    PluginEvent.Type.RELOADED
            );

            PluginManifest manifest = createManifest();
            listener.onPluginEvent(PluginEvent.installed(manifest));
            listener.onPluginEvent(PluginEvent.reloaded(manifest));
            listener.onPluginEvent(PluginEvent.removed(manifest));

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.RELOADED);
        }

        @Test
        void onReloadShouldOnlyReceiveReloadEvents() {
            List<PluginEvent> receivedEvents = new ArrayList<>();
            PluginListener listener = PluginListener.onReload(receivedEvents::add);

            PluginManifest manifest = createManifest();
            listener.onPluginEvent(PluginEvent.installed(manifest));
            listener.onPluginEvent(PluginEvent.reloaded(manifest));

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.RELOADED);
        }

        @Test
        void onInstallShouldOnlyReceiveInstallEvents() {
            List<PluginEvent> receivedEvents = new ArrayList<>();
            PluginListener listener = PluginListener.onInstall(receivedEvents::add);

            PluginManifest manifest = createManifest();
            listener.onPluginEvent(PluginEvent.installed(manifest));
            listener.onPluginEvent(PluginEvent.reloaded(manifest));

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.INSTALLED);
        }

        @Test
        void onRemoveShouldOnlyReceiveRemoveEvents() {
            List<PluginEvent> receivedEvents = new ArrayList<>();
            PluginListener listener = PluginListener.onRemove(receivedEvents::add);

            PluginManifest manifest = createManifest();
            listener.onPluginEvent(PluginEvent.installed(manifest));
            listener.onPluginEvent(PluginEvent.removed(manifest));

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.REMOVED);
        }

        @Test
        void forTypesShouldAcceptMultipleTypes() {
            List<PluginEvent> receivedEvents = new ArrayList<>();
            PluginListener listener = PluginListener.forTypes(
                    receivedEvents::add,
                    PluginEvent.Type.INSTALLED,
                    PluginEvent.Type.REMOVED
            );

            PluginManifest manifest = createManifest();
            listener.onPluginEvent(PluginEvent.installed(manifest));
            listener.onPluginEvent(PluginEvent.reloaded(manifest));
            listener.onPluginEvent(PluginEvent.removed(manifest));

            assertThat(receivedEvents).hasSize(2);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.INSTALLED);
            assertThat(receivedEvents.get(1).type()).isEqualTo(PluginEvent.Type.REMOVED);
        }
    }


    @Nested
    class PluginIdTests {

        @Test
        void pluginIdShouldBeExtractedFromManifest() {
            PluginManifest manifest = createManifest();
            PluginID id = manifest.id();

            assertThat(id.group()).isEqualTo("com.example");
            assertThat(id.name()).isEqualTo("test-plugin");
        }

        @Test
        void pluginIdEqualityShouldWork() {
            PluginID id1 = new PluginID("com.example", "plugin");
            PluginID id2 = new PluginID("com.example", "plugin");
            PluginID id3 = new PluginID("com.other", "plugin");

            assertThat(id1)
                .isEqualTo(id2)
                .isNotEqualTo(id3)
                .hasSameHashCodeAs(id2);
        }
    }


    @Nested
    @DisplayName("Plugin Lifecycle Integration Tests")
    class PluginLifecycleIntegrationTests {

        private static final Path TEST_BUNDLE = Path.of("src/test/resources/plugin.zip");
        private static final PluginID PLUGIN_ID = new PluginID("Plugin-Group", "Plugin-Name");

        @TempDir
        Path tempDir;

        private PluginManager pluginManager;
        private List<PluginEvent> receivedEvents;

        @BeforeEach
        void setUp() {
            pluginManager = new PluginManager("Plugin-Application", TestPluginHotReload.class.getClassLoader(), tempDir);
            receivedEvents = new ArrayList<>();
            pluginManager.addListener(receivedEvents::add);
        }


        @Test
        @DisplayName("should emit INSTALLED event when plugin is installed")
        void shouldEmitInstalledEventOnInstall() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.INSTALLED);
            assertThat(receivedEvents.get(0).pluginId()).isEqualTo(PLUGIN_ID);
        }


        @Test
        @DisplayName("should emit REMOVED event when plugin is removed")
        void shouldEmitRemovedEventOnRemove() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            receivedEvents.clear();

            pluginManager.removePlugin(PLUGIN_ID);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.REMOVED);
            assertThat(receivedEvents.get(0).pluginId()).isEqualTo(PLUGIN_ID);
        }


        @Test
        @DisplayName("should clear plugin from registry after removal")
        void shouldClearPluginFromRegistryAfterRemoval() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);

            // Verify plugin is present
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
            assertThat(pluginManager.getPluginManifest(PLUGIN_ID)).isPresent();

            // Remove plugin
            pluginManager.removePlugin(PLUGIN_ID);

            // Verify plugin is gone
            assertThat(pluginManager.plugins()).doesNotContain(PLUGIN_ID);
            assertThat(pluginManager.getPluginManifest(PLUGIN_ID)).isEmpty();
        }


        @Test
        @DisplayName("should emit UNLOADED and RELOADED events on reload")
        void shouldEmitUnloadedAndReloadedEventsOnReload() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            receivedEvents.clear();

            pluginManager.reloadPlugin(PLUGIN_ID);

            assertThat(receivedEvents).hasSize(2);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.UNLOADED);
            assertThat(receivedEvents.get(1).type()).isEqualTo(PluginEvent.Type.RELOADED);
        }


        @Test
        @DisplayName("should maintain plugin state after reload")
        void shouldMaintainPluginStateAfterReload() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);

            pluginManager.reloadPlugin(PLUGIN_ID);

            // Plugin should still be present
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
            assertThat(pluginManager.getPluginManifest(PLUGIN_ID)).isPresent();
        }


        @Test
        @DisplayName("should handle multiple reload cycles")
        void shouldHandleMultipleReloadCycles() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);

            // Multiple reloads
            for (int i = 0; i < 3; i++) {
                pluginManager.reloadPlugin(PLUGIN_ID);
                assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
            }

            // Should have: 1 install + 3 * (1 unload + 1 reload) = 7 events
            assertThat(receivedEvents).hasSize(7);
        }


        @Test
        @DisplayName("reloadAllPlugins should reload all installed plugins")
        void reloadAllPluginsShouldReloadAll() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            receivedEvents.clear();

            pluginManager.reloadAllPlugins();

            // Should have unload + reload for the plugin
            assertThat(receivedEvents).hasSize(2);
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
        }


        @Test
        @DisplayName("should throw when reloading non-existent plugin")
        void shouldThrowWhenReloadingNonExistentPlugin() {
            PluginID nonExistent = new PluginID("non", "existent");

            assertThatThrownBy(() -> pluginManager.reloadPlugin(nonExistent))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("is not installed");
        }


        @Test
        @DisplayName("should allow re-installation after removal")
        void shouldAllowReinstallationAfterRemoval() {
            // Install
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);

            // Remove
            pluginManager.removePlugin(PLUGIN_ID);
            assertThat(pluginManager.plugins()).doesNotContain(PLUGIN_ID);

            // Re-install
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
        }


        @Test
        @DisplayName("should refresh all plugins from disk")
        void shouldRefreshAllPluginsFromDisk() {
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);

            // Refresh
            pluginManager.refresh();

            // Plugin should still be there (re-discovered from disk)
            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
        }


        @Test
        @DisplayName("should remove listener successfully")
        void shouldRemoveListenerSuccessfully() {
            List<PluginEvent> separateEvents = new ArrayList<>();
            PluginListener separateListener = separateEvents::add;

            pluginManager.addListener(separateListener);

            // Install - both listeners should receive
            pluginManager.installPluginFromBundle(TEST_BUNDLE);

            assertThat(separateEvents).hasSize(1);

            // Remove the separate listener
            boolean removed = pluginManager.removeListener(separateListener);
            assertThat(removed).isTrue();

            // Remove and reinstall - only main listener should receive new events
            separateEvents.clear();
            pluginManager.removePlugin(PLUGIN_ID);

            assertThat(separateEvents).isEmpty();
            assertThat(receivedEvents).anyMatch(e -> e.type() == PluginEvent.Type.REMOVED);
        }


        @Test
        @DisplayName("full lifecycle: install -> query -> remove -> verify clean")
        void fullLifecycleTest() {
            // 1. Install
            pluginManager.installPluginFromBundle(TEST_BUNDLE);

            assertThat(pluginManager.plugins()).contains(PLUGIN_ID);
            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.INSTALLED);

            // 2. Query - plugin should be available
            assertThat(pluginManager.getPluginManifest(PLUGIN_ID)).isPresent();
            assertThat(pluginManager.getPluginManifest(PLUGIN_ID).get().version().toString()).isEqualTo("3.12");

            // 3. Remove
            receivedEvents.clear();
            pluginManager.removePlugin(PLUGIN_ID);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.REMOVED);

            // 4. Verify clean - plugin should be completely gone
            assertThat(pluginManager.plugins()).doesNotContain(PLUGIN_ID);
            assertThat(pluginManager.getPluginManifest(PLUGIN_ID)).isEmpty();

            // Manifest file should be deleted
            Path manifestFile = tempDir.resolve("manifests/Plugin-Group-Plugin-Name.yaml");
            assertThat(manifestFile).doesNotExist();
        }


        @Test
        @DisplayName("should track events across full install-remove-reinstall cycle")
        void shouldTrackEventsAcrossFullCycle() {
            // Install
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).type()).isEqualTo(PluginEvent.Type.INSTALLED);

            // Remove
            pluginManager.removePlugin(PLUGIN_ID);
            assertThat(receivedEvents).hasSize(2);
            assertThat(receivedEvents.get(1).type()).isEqualTo(PluginEvent.Type.REMOVED);

            // Reinstall
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            assertThat(receivedEvents).hasSize(3);
            assertThat(receivedEvents.get(2).type()).isEqualTo(PluginEvent.Type.INSTALLED);
        }


        @Test
        @DisplayName("should clean up module layers after plugin removal")
        void shouldCleanUpModuleLayersAfterRemoval() {
            // Initially no layers
            long initialLayers = pluginManager.moduleLayers().count();

            // Install plugin
            pluginManager.installPluginFromBundle(TEST_BUNDLE);
            long afterInstall = pluginManager.moduleLayers().count();

            // After remove, should return to initial state
            pluginManager.removePlugin(PLUGIN_ID);
            long afterRemove = pluginManager.moduleLayers().count();

            assertThat(afterRemove).isEqualTo(initialLayers);
            // Note: afterInstall may or may not be > initialLayers depending on
            // whether the JARs contain valid Java module descriptors
        }
    }
}
