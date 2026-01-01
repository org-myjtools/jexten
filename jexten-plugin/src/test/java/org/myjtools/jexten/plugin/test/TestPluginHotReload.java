package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.jexten.plugin.PluginEvent;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginListener;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


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

            assertThat(id1).isEqualTo(id2);
            assertThat(id1).isNotEqualTo(id3);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }
    }
}
