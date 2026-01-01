# JExten Maven Artifact Store

Maven repository integration for JExten plugin artifact resolution.

## Overview

This module provides an `ArtifactStore` implementation that resolves plugin artifacts from Maven repositories. It enables plugins to specify dependencies using Maven coordinates, with automatic:

- Artifact resolution from local and remote Maven repositories
- Transitive dependency resolution
- Artifact caching in the local Maven repository

## Installation

```xml
<dependency>
    <groupId>org.myjtools.jexten</groupId>
    <artifactId>jexten-maven-artifact-store</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```java
import org.myjtools.jexten.maven.artifactstore.MavenArtifactStore;
import org.myjtools.jexten.plugin.PluginManager;

// Create Maven artifact store
MavenArtifactStore artifactStore = new MavenArtifactStore();

// Create plugin manager with Maven support
PluginManager pluginManager = new PluginManager(
    Path.of("plugins"),
    artifactStore
);

// Load plugins - dependencies resolved from Maven
pluginManager.loadPlugins();
```

## Configuration

Configure the artifact store using properties:

```java
Properties props = new Properties();

// Local repository location
props.setProperty("localRepository", "/path/to/local/repo");

// Remote repositories (comma-separated)
props.setProperty("remoteRepositories",
    "https://repo.maven.apache.org/maven2," +
    "https://my-company.com/maven");

// Proxy configuration (optional)
props.setProperty("proxy.host", "proxy.company.com");
props.setProperty("proxy.port", "8080");

// Authentication (optional)
props.setProperty("repository.myrepo.username", "user");
props.setProperty("repository.myrepo.password", "pass");

MavenArtifactStore store = new MavenArtifactStore()
    .configure(props);
```

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `localRepository` | Path to local Maven repository | `~/.m2/repository` |
| `remoteRepositories` | Comma-separated list of remote repository URLs | Maven Central |
| `proxy.host` | HTTP proxy hostname | - |
| `proxy.port` | HTTP proxy port | - |
| `proxy.username` | Proxy authentication username | - |
| `proxy.password` | Proxy authentication password | - |
| `offline` | Work offline (use only local cache) | `false` |

## Plugin Manifest Format

When using Maven artifact store, plugin manifests specify dependencies using a simplified format:

```yaml
group: com.example
name: my-plugin
version: 1.0.0
hostModule: com.example.myplugin

artifacts:
  # Format: groupId -> list of "artifactId-version"
  org.apache.commons:
    - commons-lang3-3.12.0
    - commons-io-2.11.0
  com.google.guava:
    - guava-31.1-jre
```

The store resolves these to full Maven coordinates and fetches all transitive dependencies.

## Dependency Scopes

The artifact store fetches dependencies with the following scopes:

| Scope | Included |
|-------|----------|
| `compile` | Yes |
| `runtime` | Yes |
| `provided` | No |
| `test` | No |
| `system` | No |

## Integration Example

Complete example with custom repository:

```java
import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.maven.artifactstore.MavenArtifactStore;
import org.myjtools.jexten.plugin.PluginManager;

public class Application {

    public static void main(String[] args) {
        // Configure Maven artifact store
        Properties mavenConfig = new Properties();
        mavenConfig.setProperty("remoteRepositories",
            "https://repo.maven.apache.org/maven2," +
            "https://plugins.mycompany.com/maven");

        MavenArtifactStore artifactStore = new MavenArtifactStore()
            .configure(mavenConfig);

        // Create plugin manager
        PluginManager pluginManager = new PluginManager(
            Path.of("plugins"),
            artifactStore
        );

        // Load all plugins
        pluginManager.loadPlugins();

        // Create extension manager with plugin support
        ExtensionManager extensions = ExtensionManager.create(pluginManager);

        // Use extensions from both app and plugins
        extensions.getExtensions(MyService.class)
            .forEach(MyService::execute);
    }
}
```

## Caching Behavior

Artifacts are cached according to Maven conventions:

- **Release versions**: Cached permanently in local repository
- **SNAPSHOT versions**: Re-checked based on update policy
- **Missing artifacts**: Cached for configured timeout

## Troubleshooting

### "Could not resolve artifact"

1. Check network connectivity
2. Verify repository URLs are correct
3. Check if artifact exists in configured repositories
4. For private repositories, verify authentication

### "Connection refused"

1. Check proxy configuration if behind corporate firewall
2. Verify repository URL is accessible

### Slow resolution

1. Use a local Maven repository mirror
2. Configure offline mode for development
3. Check network latency to remote repositories

## Documentation

For complete documentation, see the [main JExten README](../README.md).

## License

Apache License 2.0
