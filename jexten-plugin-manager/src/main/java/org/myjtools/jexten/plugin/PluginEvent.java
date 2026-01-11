package org.myjtools.jexten.plugin;

/**
 * Represents an event that occurred during the plugin lifecycle.
 * <p>
 * Events are emitted by the {@link PluginManager} when plugins are
 * installed, unloaded, or reloaded.
 *
 * @see PluginListener
 */
public record PluginEvent(
    Type type,
    PluginID pluginId,
    PluginManifest manifest
) {

    /**
     * The type of plugin event.
     */
    public enum Type {
        /**
         * A plugin was successfully installed and loaded.
         */
        INSTALLED,

        /**
         * A plugin was unloaded (removed or being reloaded).
         */
        UNLOADED,

        /**
         * A plugin was reloaded (unloaded and loaded again).
         */
        RELOADED,

        /**
         * A plugin was removed from the system.
         */
        REMOVED
    }


    /**
     * Creates an INSTALLED event.
     */
    public static PluginEvent installed(PluginManifest manifest) {
        return new PluginEvent(Type.INSTALLED, manifest.id(), manifest);
    }


    /**
     * Creates an UNLOADED event.
     */
    public static PluginEvent unloaded(PluginManifest manifest) {
        return new PluginEvent(Type.UNLOADED, manifest.id(), manifest);
    }


    /**
     * Creates a RELOADED event.
     */
    public static PluginEvent reloaded(PluginManifest manifest) {
        return new PluginEvent(Type.RELOADED, manifest.id(), manifest);
    }


    /**
     * Creates a REMOVED event.
     */
    public static PluginEvent removed(PluginManifest manifest) {
        return new PluginEvent(Type.REMOVED, manifest.id(), manifest);
    }
}
