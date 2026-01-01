/**
 * Plugin management module for the JExten extension framework.
 * <p>
 * This module provides runtime plugin loading capabilities, enabling applications
 * to dynamically discover and load plugins from external sources. Key features:
 * <ul>
 *   <li>{@code PluginManager} for loading and managing plugin lifecycle</li>
 *   <li>{@code PluginManifest} for describing plugin metadata (YAML format)</li>
 *   <li>{@code ArtifactStore} SPI for custom plugin artifact resolution</li>
 *   <li>Support for isolated module layers per plugin</li>
 * </ul>
 *
 * @see org.myjtools.jexten.plugin.PluginManager
 * @see org.myjtools.jexten.plugin.PluginManifest
 */
module org.myjtools.jexten.plugin {

    // Public API: Plugin management classes and interfaces
    exports org.myjtools.jexten.plugin;

    // Internal API: Exposed for advanced use cases (e.g., custom artifact stores)
    // Contains internal implementation details that may change between versions
    exports org.myjtools.jexten.plugin.internal;

    // Core JExten framework dependency
    requires org.myjtools.jexten;

    // YAML parsing for plugin manifest files (plugin.yml)
    requires org.yaml.snakeyaml;

    // Logging support
    requires org.slf4j;

    // HTTP client for downloading remote plugins
    requires java.net.http;

    // Compiler API for module layer creation and management
    requires jdk.compiler;

    // Opens packages to SnakeYAML for reflective access during YAML deserialization.
    // Required because SnakeYAML uses reflection to instantiate and populate
    // PluginManifest and related configuration classes from YAML content.
    opens org.myjtools.jexten.plugin to org.yaml.snakeyaml;
    opens org.myjtools.jexten.plugin.internal to org.yaml.snakeyaml;
}
