# JExten Core

Core API module for the JExten extension framework.

## Overview

This module provides the fundamental API for defining and discovering extensions in modular Java applications. It contains:

- `@Extension` and `@ExtensionPoint` annotations
- `ExtensionManager` for discovering and retrieving extensions
- `Scope` and `Priority` enums for lifecycle and ordering control
- `InjectionProvider` and `ExtensionLoader` for DI integration

## Installation

```xml
<dependency>
    <groupId>org.myjtools.jexten</groupId>
    <artifactId>jexten-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Define an Extension Point

```java
@ExtensionPoint(version = "1.0")
public interface MessageFormatter {
    String format(String message);
}
```

### Implement an Extension

```java
@Extension
public class UpperCaseFormatter implements MessageFormatter {
    @Override
    public String format(String message) {
        return message.toUpperCase();
    }
}
```

### Discover Extensions

```java
ExtensionManager manager = ExtensionManager.create();

// Get highest priority extension
Optional<MessageFormatter> formatter = manager.getExtension(MessageFormatter.class);

// Get all extensions
manager.getExtensions(MessageFormatter.class)
    .forEach(f -> System.out.println(f.format("hello")));
```

## Module Configuration

Your `module-info.java` must declare the extension point and implementations:

```java
module my.application {
    requires org.myjtools.jexten;

    exports com.example.api;  // Package containing extension point
    opens com.example.api to org.myjtools.jexten;

    uses com.example.api.MessageFormatter;
    provides com.example.api.MessageFormatter
        with com.example.impl.UpperCaseFormatter;
}
```

## Extension Configuration

### Priority

Control which extension is selected when multiple exist:

```java
@Extension(priority = Priority.HIGHEST)
public class PreferredFormatter implements MessageFormatter { }
```

Priority levels: `LOWEST`, `LOW`, `NORMAL` (default), `HIGH`, `HIGHEST`

### Scope

Control instance lifecycle:

```java
@Extension(scope = Scope.SINGLETON)  // Single shared instance
@Extension(scope = Scope.LOCAL)      // One per context (default)
@Extension(scope = Scope.TRANSIENT)  // New instance per request
```

### Named Extensions

Identify extensions by name:

```java
@Extension(name = "json")
public class JsonFormatter implements MessageFormatter { }

// Retrieve by name
manager.getExtensionByName(MessageFormatter.class, "json");
```

## Dependency Injection

Inject dependencies into extensions using `@Inject`:

```java
@Extension
public class LoggingFormatter implements MessageFormatter {
    @Inject
    private Logger logger;

    @Override
    public String format(String message) {
        logger.info("Formatting: {}", message);
        return message;
    }
}
```

### Custom Injection Provider

Integrate with external DI frameworks:

```java
ExtensionManager manager = ExtensionManager.create()
    .withInjectionProvider(new SpringInjectionProvider(applicationContext));
```

## Documentation

For complete documentation, see the [main JExten README](../README.md).

## License

Apache License 2.0
