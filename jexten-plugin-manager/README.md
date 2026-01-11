# JExten Plugin

Runtime plugin management module for the JExten extension framework.

## Overview

This module provides dynamic plugin loading capabilities, enabling applications to discover and load plugins at runtime from external sources. Key features:

- `PluginManager` for loading and managing plugin lifecycle
- `PluginManifest` for describing plugin metadata (YAML format)
- `ArtifactStore` SPI for custom plugin artifact resolution
- Isolated module layers per plugin for dependency isolation

## Installation

```xml
<dependency>
    <groupId>org.myjtools.jexten</groupId>
    <artifactId>jexten-plugin</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Load Plugins

```java
// Create plugin manager with a plugins directory
PluginManager pluginManager = new PluginManager(Path.of("plugins"));

// Load all plugins from the directory
pluginManager.loadPlugins();

// Create extension manager with plugin support
ExtensionManager extensions = ExtensionManager.create(pluginManager);

// Discover extensions from both application and plugins
extensions.getExtensions(MyExtensionPoint.class)
    .forEach(ext -> ext.execute());
```

### Install a Plugin

```java
// From a local file
pluginManager.install(Path.of("my-plugin-1.0.zip"));

// From a URL
pluginManager.install(new URL("https://example.com/plugins/my-plugin-1.0.zip"));
```

### Remove a Plugin

```java
pluginManager.remove(new PluginID("com.example", "my-plugin"));
```

## Plugin Manifest

Each plugin requires a `plugin.yaml` manifest file:

```yaml
group: com.example
name: my-plugin
version: 1.0.0
hostModule: com.example.myplugin

# Optional metadata
displayName: My Awesome Plugin
description: A plugin that does awesome things
application: MyApplication
url: https://example.com/my-plugin
licenseName: Apache-2.0

# Artifact dependencies (JAR files)
artifacts:
  main:
    - my-plugin-1.0.0.jar
    - dependency-lib-2.0.jar

# Extension declarations
extensions:
  com.example.api.MyExtensionPoint:
    - com.example.plugin.MyExtensionImpl

# Extension points provided by this plugin
extensionPoints:
  - com.example.plugin.api.PluginExtensionPoint
```

### Required Fields

| Field | Description |
|-------|-------------|
| `group` | Plugin group identifier (like Maven groupId) |
| `name` | Plugin name (like Maven artifactId) |
| `version` | Semantic version (e.g., `1.0.0`, `2.1.0-SNAPSHOT`) |
| `hostModule` | JPMS module name of the main plugin module |

## Plugin Package Formats

### ZIP Bundle

A ZIP file containing:
```
my-plugin-1.0.zip
├── plugin.yaml
├── my-plugin-1.0.0.jar
├── dependency-lib-2.0.jar
└── other-dependency.jar
```

### JAR with Embedded Manifest

A JAR file with `plugin.yaml` at the root:
```
my-plugin-1.0.jar
├── plugin.yaml
├── com/example/plugin/...
└── META-INF/...
```

## Artifact Stores

Artifact stores resolve plugin dependencies. Built-in options:

### Local Directory Store (Default)

Resolves artifacts from the plugin package itself.

### Maven Artifact Store

Resolves artifacts from Maven repositories:

```xml
<dependency>
    <groupId>org.myjtools.jexten</groupId>
    <artifactId>jexten-maven-artifact-store</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
ArtifactStore mavenStore = new MavenArtifactStore(
    Path.of("plugin-cache"),
    List.of("https://repo.maven.apache.org/maven2")
);

PluginManager pluginManager = new PluginManager(
    Path.of("plugins"),
    mavenStore
);
```

### Custom Artifact Store

Implement the `ArtifactStore` interface:

```java
public class MyArtifactStore implements ArtifactStore {
    @Override
    public List<Path> resolve(String artifact) {
        // Resolve artifact to local paths
    }
}
```

## Plugin Lifecycle

```
┌─────────┐     ┌─────────┐     ┌────────┐     ┌──────────┐
│ Install │────▶│  Load   │────▶│ Active │────▶│ Unloaded │
└─────────┘     └─────────┘     └────────┘     └──────────┘
                     │               │
                     ▼               ▼
               ┌──────────┐    ┌─────────┐
               │  Failed  │    │ Removed │
               └──────────┘    └─────────┘
```

## Module Layer Isolation

Each plugin runs in its own JPMS module layer, providing:

- **Dependency isolation**: Plugins can use different versions of the same library
- **Security**: Plugins cannot access internal APIs of other plugins
- **Hot reload**: Plugins can be loaded/unloaded without restarting the application

## Documentation

For complete documentation, see the [main JExten README](../README.md).

## License

Apache License 2.0
