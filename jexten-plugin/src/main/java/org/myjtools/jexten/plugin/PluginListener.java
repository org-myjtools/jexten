package org.myjtools.jexten.plugin;

/**
 * Listener interface for receiving plugin lifecycle events.
 * <p>
 * Implementations can react to plugin installation, unloading, reloading,
 * and removal events. This is useful for:
 * <ul>
 *   <li>Clearing caches when plugins are reloaded</li>
 *   <li>Updating UI components that display plugin information</li>
 *   <li>Logging plugin lifecycle events</li>
 *   <li>Notifying dependent systems of plugin changes</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * pluginManager.addListener(event -> {
 *     switch (event.type()) {
 *         case INSTALLED -> log.info("Plugin installed: {}", event.pluginId());
 *         case RELOADED -> {
 *             log.info("Plugin reloaded: {}", event.pluginId());
 *             extensionManager.clear(); // Clear extension cache
 *         }
 *         case REMOVED -> log.info("Plugin removed: {}", event.pluginId());
 *     }
 * });
 * }</pre>
 *
 * @see PluginEvent
 * @see PluginManager#addListener(PluginListener)
 */
@FunctionalInterface
public interface PluginListener {

    /**
     * Called when a plugin lifecycle event occurs.
     *
     * @param event the plugin event
     */
    void onPluginEvent(PluginEvent event);


    /**
     * Creates a listener that only responds to specific event types.
     *
     * @param types the event types to listen for
     * @param listener the listener to invoke for matching events
     * @return a filtered listener
     */
    static PluginListener forTypes(PluginListener listener, PluginEvent.Type... types) {
        var typeSet = java.util.Set.of(types);
        return event -> {
            if (typeSet.contains(event.type())) {
                listener.onPluginEvent(event);
            }
        };
    }


    /**
     * Creates a listener that responds only to RELOADED events.
     * <p>
     * This is a convenience method for the common case of reacting to plugin reloads.
     *
     * @param listener the listener to invoke on reload
     * @return a listener that only responds to reload events
     */
    static PluginListener onReload(PluginListener listener) {
        return forTypes(listener, PluginEvent.Type.RELOADED);
    }


    /**
     * Creates a listener that responds only to INSTALLED events.
     *
     * @param listener the listener to invoke on install
     * @return a listener that only responds to install events
     */
    static PluginListener onInstall(PluginListener listener) {
        return forTypes(listener, PluginEvent.Type.INSTALLED);
    }


    /**
     * Creates a listener that responds only to REMOVED events.
     *
     * @param listener the listener to invoke on removal
     * @return a listener that only responds to removal events
     */
    static PluginListener onRemove(PluginListener listener) {
        return forTypes(listener, PluginEvent.Type.REMOVED);
    }
}
