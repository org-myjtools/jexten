# JExten

**JExten** is a lightweight, annotation-driven extension framework for Java that leverages the Java Platform Module System (JPMS) to create dynamic, plugin-based applications.

## Features

- **JPMS-Native**: Built from the ground up on Java's Module System (Java 21+)
- **Annotation-Driven**: Simple `@ExtensionPoint` and `@Extension` annotations
- **Semantic Versioning**: Built-in version compatibility checking
- **Dependency Injection**: Field-level injection with `@Inject` annotation
- **Dynamic Loading**: Install/remove plugins at runtime without restart
- **Priority System**: Deterministic extension resolution with priority levels
- **Scoped Instances**: SINGLETON, SESSION, and TRANSIENT lifecycle management
- **Plugin Integrity**: SHA-256 checksums for artifact verification
- **Maven & Gradle Support**: First-class build tool integration

## Quick Start

### 1. Add Dependencies

```xml
<dependency>
    <groupId>org.myjtools</groupId>
    <artifactId>jexten-core</artifactId>
    <version>1.0.0-alpha1</version>
</dependency>

<!-- For plugin management -->
<dependency>
    <groupId>org.myjtools</groupId>
    <artifactId>jexten-plugin-manager</artifactId>
    <version>1.0.0-alpha1</version>
</dependency>
```

### 2. Define an Extension Point

```java
import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint(version = "1.0")
public interface Greeter {
    void greet(String name);
}
```

### 3. Implement an Extension

```java
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Priority;
import org.myjtools.jexten.Scope;

@Extension
public class FriendlyGreeter implements Greeter {
    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name + "!");
    }
}
```

### 4. Configure module-info.java

The JExten annotation processor will check the necessary configuration in your `module-info.java` file, and it will
suggest you any missing `uses` or `provides` statements at compile time. Here is an example of how it should look:

```java
module com.example.app {
    requires org.myjtools.jexten;

    exports com.example;

    uses com.example.Greeter;
    provides com.example.Greeter with com.example.FriendlyGreeter;
}
```


### 5. Discover and Use Extensions

```java
import org.myjtools.jexten.ExtensionManager;

public class Main {
    public static void main(String[] args) {
        ExtensionManager manager = ExtensionManager.create();

        // Get the highest priority extension
        manager.getExtension(Greeter.class)
            .ifPresent(greeter -> greeter.greet("World"));

        // Get all extensions
        manager.getExtensions(Greeter.class)
            .forEach(greeter -> greeter.greet("Everyone"));
    }
}
```

## Assembling Plugins

JExten supports dynamic plugin loading via `PluginManager`. A plugin is a collection of extensions packaged as a JPMS
module in a ZIP bundle that contains all required artifacts along with the `plugin.yaml` definition file.

You can use the following configuration to automatically generate plugin bundles with Maven:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.myjtools.jexten</groupId>
                        <artifactId>jexten-processor</artifactId>
                        <version>1.0.0-alpha1</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.myjtools.jexten</groupId>
            <artifactId>jexten-maven-plugin</artifactId>
            <version>1.0.0-alpha1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate-manifest</goal>
                        <goal>assemble-bundle</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <application>org.myjtools.jexten.example.app</application>
                <hostModule>org.myjtools.jexten.example.app</hostModule>
                <hostArtifact>org.myjtools.jexten.example:jexten-example-app</hostArtifact>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Loading Plugins at Runtime

```java
import org.myjtools.jexten.plugin.PluginManager;
import org.myjtools.jexten.ExtensionManager;

public class Application {
    public static void main(String[] args) throws IOException {
        Path pluginDir = Path.of("plugins");

        // Create plugin manager
        PluginManager pluginManager = new PluginManager(
            "org.myjtools.jexten.example.app",  // Application ID
            Application.class.getClassLoader(),
            pluginDir
        );

        // Install plugin from bundle
        pluginManager.installPluginFromBundle(
            pluginDir.resolve("my-plugin-1.0.0.zip")
        );

        // Create extension manager with plugin support
        ExtensionManager extensionManager = ExtensionManager.create(pluginManager);

        // Discover extensions from plugins
        extensionManager.getExtensions(Greeter.class)
            .forEach(g -> g.greet("Plugin User"));

        // Remove plugin at runtime
        pluginManager.removePlugin(new PluginID("com.example", "my-plugin"));
    }
}
```

## Dependency Injection

JExten provides built-in dependency injection:

```java
@Extension(extensionPoint = "com.example.Service")
public class MyService implements Service {

    @Inject
    private Repository repository;  // Inject another extension

    @Inject(value = "special")
    private Logger logger;          // Named injection

    @Inject
    private List<Plugin> plugins;   // Inject all implementations

    @PostConstruct
    void initialize() {
        // Called after all injections complete
    }
}
```

## Plugin Manifest

Plugins are defined with a `plugin.yaml` manifest:

```yaml
application: com.example.app
group: com.example
name: my-plugin
version: '1.0'
displayName: My Plugin
description: A sample plugin for the application
hostModule: com.example.my.plugin
url: https://github.com/example/my-plugin
licenseName: MIT
licenseText: MIT License

artifacts:
  com.example:
    - my-plugin-1.0.0
    - dependency-lib-2.3.1

extensions:
  com.example.Greeter:
    - com.example.plugin.CustomGreeter

extensionPoints:
  - com.example.plugin.NewExtensionPoint

# SHA-256 checksums for integrity verification (auto-generated by jexten-maven-plugin)
checksums:
  my-plugin-1.0.0.jar: a1b2c3d4e5f6...
  dependency-lib-2.3.1.jar: f6e5d4c3b2a1...
```

The `checksums` field contains SHA-256 hashes for each artifact in the bundle. These are automatically generated by `jexten-maven-plugin` during packaging and verified by `PluginManager` when loading plugins to ensure artifact integrity.

## Plugin Integrity Verification

JExten includes built-in integrity verification for plugin artifacts using SHA-256 checksums.

### How It Works

1. **During packaging**: `jexten-maven-plugin` calculates SHA-256 checksums for all artifacts included in the bundle and embeds them in `plugin.yaml`

2. **During installation**: When `installPluginFromBundle()` is called, checksums are verified immediately after extraction

3. **During discovery**: When the application starts, installed plugins are verified against their stored checksums

### Verification Failures

If an artifact's checksum doesn't match, `PluginManager` throws a `PluginException`:

```
PluginException: Checksum verification failed for plugin com.example:my-plugin :
artifact my-plugin-1.0.0.jar has been modified or corrupted
(expected a1b2c3..., found d4e5f6...); please reinstall the plugin
```

### Backwards Compatibility

Plugins without checksums (created with older versions of `jexten-maven-plugin`) continue to work normally - integrity verification is skipped when no checksums are present.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Host Application                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              ExtensionManager                           ││
│  │   ┌─────────────┐  ┌─────────────┐  ┌───────────┐       ││
│  │   │ServiceLoader│  │InjectionHdlr│  │   Cache   │       ││
│  │   └─────────────┘  └─────────────┘  └───────────┘       ││
│  └─────────────────────────────────────────────────────────┘│
│                            │                                │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              PluginManager                              ││
│  │   ┌─────────────┐  ┌─────────────┐  ┌───────────┐       ││
│  │   │ ModuleLayer │  │  Manifest   │  │ Artifacts │       ││
│  │   │    Tree     │  │   Parser    │  │   Store   │       ││
│  │   └─────────────┘  └─────────────┘  └───────────┘       ││
│  └─────────────────────────────────────────────────────────┘│
│                            │                                │
├────────────────────────────┼────────────────────────────────┤
│          Boot Layer        │         Plugin Layers          │
│  ┌───────────────────┐     │    ┌───────────────────┐       │
│  │   Host Module     │     │    │    Plugin A       │       │
│  │  @ExtensionPoint  │◄────┼────│   @Extension      │       │
│  └───────────────────┘     │    └───────────────────┘       │
│                            │    ┌───────────────────┐       │
│                            │    │    Plugin B       │       │
│                            │    │   @Extension      │       │
│                            │    └───────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

## Comparison with Other Frameworks

### JExten vs OSGi vs Layrry

| Feature | JExten | OSGi | Layrry |
|---------|--------|------|--------|
| **Foundation** | JPMS (Java 21+) | Custom classloaders | JPMS |
| **Configuration** | Annotations + YAML | Manifest headers | YAML/TOML/API |
| **Learning Curve** | Low | High | Medium |
| **Runtime Weight** | Lightweight | Heavy | Lightweight |
| **Versioning** | Semantic (built-in) | Full version ranges | Maven coordinates |
| **Dynamic Loading** | Yes | Yes | Yes |
| **Dependency Injection** | Built-in | Declarative Services | No |
| **Service Model** | ServiceLoader + Extensions | OSGi Services | ServiceLoader |
| **Build Integration** | Maven/Gradle plugins | BND tools | Maven/Gradle |
| **Maturity** | New | 20+ years | 2020+ |

### When to Choose JExten

**Choose JExten when you need:**
- A modern, JPMS-native solution for Java 21+
- Simple annotation-based configuration
- Built-in dependency injection without external frameworks
- Semantic versioning with automatic compatibility checks
- Priority-based extension resolution
- Low learning curve and minimal boilerplate

**Choose OSGi when you need:**
- Battle-tested solution with 20+ years of production use
- Complex version range dependencies
- Advanced service dynamics (service ranking, filters)
- Compatibility with older Java versions
- Enterprise-grade tooling (Eclipse PDE, BND)

**Choose Layrry when you need:**
- Configuration-first approach (YAML/TOML)
- JBang integration for quick prototyping
- Multiple versions of the same module simultaneously
- File-watching for automatic plugin detection
- Minimal API footprint

### Detailed Comparison

#### OSGi

[OSGi](https://www.osgi.org/) is the veteran of Java modularity, predating JPMS by nearly two decades. It provides comprehensive modularity features including per-bundle classloaders, dynamic services, and sophisticated version ranges.

**Advantages over JExten:**
- Mature ecosystem with extensive tooling
- Fine-grained package-level exports
- Complex dependency resolution (version ranges, optional imports)
- Works on any Java version

**Disadvantages vs JExten:**
- Steep learning curve
- Heavy runtime footprint
- Requires OSGi container
- Complex manifest configuration
- Not integrated with JPMS

#### Layrry

[Layrry](https://github.com/moditect/layrry) is a modern launcher for layered Java applications, created by Gunnar Morling. It provides YAML/TOML-based configuration for module layer hierarchies.

**Advantages over JExten:**
- Configuration-only approach (no annotations required)
- Built-in file watching for plugins directory
- JBang integration
- TOML support

**Disadvantages vs JExten:**
- No built-in dependency injection
- No semantic versioning validation
- No priority system for extensions
- Less opinionated (more configuration needed)
- No annotation processor for compile-time validation

### Migration Paths

#### From OSGi to JExten

1. Replace `@Component` with `@Extension`
2. Replace OSGi service interfaces with `@ExtensionPoint`
3. Convert `MANIFEST.MF` headers to `module-info.java`
4. Replace `@Reference` with `@Inject`
5. Update build from BND to standard Maven/Gradle

#### From Layrry to JExten

1. Add `@ExtensionPoint` annotations to service interfaces
2. Add `@Extension` annotations to implementations
3. Convert layers.yaml to plugin.yaml manifests
4. Use `PluginManager` instead of Layrry launcher

## Modules

| Module | Description |
|--------|-------------|
| `jexten-core` | Core annotations and ExtensionManager API |
| `jexten-plugin-manager` | Plugin management, dynamic loading, and integrity verification |
| `jexten-processor` | Compile-time annotation processor |
| `jexten-maven-plugin` | Maven plugin for bundling plugins with checksum generation |
| `jexten-maven-artifact-store` | Maven repository integration |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/org-myjtools/jexten.git
cd jexten

# Build with Maven
mvn clean install

# Run tests
mvn test
```

## Requirements

- Java 21 or higher
- Maven 3.9+ or Gradle 8+

## Examples

See the `examples/` directory for complete working examples:

- `jexten-example-app` - Host application demonstrating plugin loading
- `jexten-example-plugin-a` - Basic plugin implementation
- `jexten-example-plugin-b` - Plugin with custom extension points
- `jexten-example-plugin-c` - Plugin with dependencies on other plugins
- `jexten-example-plugin-c1` - Additional plugin dependency example
- `jexten-example-plugin-c2` - Additional plugin dependency example
- `jexten-example-plugin-d` - Gradle-based plugin

## License

[MIT License](LICENSE)

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting pull requests.

## Resources

- [OSGi Alliance](https://www.osgi.org/)
- [Layrry - GitHub](https://github.com/moditect/layrry)
- [Plug-in Architectures With Layrry](https://www.morling.dev/blog/plugin-architectures-with-layrry-and-the-java-module-system/)
- [Java 9, OSGi and the Future of Modularity](https://www.infoq.com/articles/java9-osgi-future-modularity/)
- [JPMS Documentation](https://openjdk.org/projects/jigsaw/)
