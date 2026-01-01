# JExten Processor

Annotation processor for compile-time validation of JExten extensions.

## Overview

This module provides an annotation processor that validates `@Extension` and `@ExtensionPoint` usage at compile time. It ensures:

- Correct annotation placement (classes for `@Extension`, interfaces for `@ExtensionPoint`)
- Proper `module-info.java` declarations (`exports`, `opens`, `provides`, `uses`)
- Valid semantic version formats
- Type hierarchy correctness (extensions must implement their extension points)

The processor also generates `META-INF` metadata files for runtime extension discovery.

## Installation

```xml
<dependency>
    <groupId>org.myjtools.jexten</groupId>
    <artifactId>jexten-processor</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

The processor is automatically discovered via `META-INF/services` and runs during compilation.

## Validations

### Extension Validation

The processor validates that extensions:

1. Are applied to classes (not interfaces or enums)
2. Implement or extend the declared extension point
3. Have valid `extensionPointVersion` format
4. Are declared in `module-info.java` with `provides`

### Extension Point Validation

The processor validates that extension points:

1. Are applied to interfaces
2. Have valid `version` format
3. Are exported in `module-info.java`
4. Are opened to `org.myjtools.jexten` for reflection
5. Are declared with `uses` directive

## Error Messages

The processor provides helpful error messages with suggested fixes:

```
error: [jexten] Extension com.example.MyExtension implementing extension point
       com.example.MyExtensionPoint must be declared in the module-info.java file

Try to apply the following fixes to the module-info.java file:
    provides com.example.MyExtensionPoint with com.example.MyExtension;
```

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Cannot find module definition` | Missing `module-info.java` | Create a `module-info.java` file |
| `@Extension ignored for X` | Applied to non-class | Move annotation to a class |
| `@ExtensionPoint not valid for X` | Applied to non-interface | Move annotation to an interface |
| `must be exported` | Package not exported | Add `exports package.name;` |
| `must be opened to org.myjtools.jexten` | Package not opened | Add `opens package.name to org.myjtools.jexten;` |
| `must be declared in module-info` | Missing `provides` | Add `provides ExtPoint with Impl;` |
| `must be a valid semantic version` | Invalid version format | Use format `major.minor[.patch]` |

## Generated Files

The processor generates metadata files in `META-INF/`:

### `META-INF/extensions`

Lists all extensions and their extension points:

```
com.example.api.MyExtensionPoint=com.example.impl.MyExtensionImpl
com.example.api.OtherPoint=com.example.impl.OtherImpl1,com.example.impl.OtherImpl2
```

### `META-INF/extension-points`

Lists all extension points defined in the module:

```
com.example.api.MyExtensionPoint
com.example.api.OtherPoint
```

## IDE Integration

The processor integrates with IDEs that support annotation processing:

### IntelliJ IDEA

Annotation processing is enabled by default. Errors appear in the editor and build output.

### Eclipse

Enable annotation processing in:
`Project Properties > Java Compiler > Annotation Processing`

### VS Code (with Java Extension Pack)

Annotation processing works automatically with the Java Language Server.

## Disabling the Processor

To disable the processor temporarily:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgument>-proc:none</compilerArgument>
    </configuration>
</plugin>
```

## Documentation

For complete documentation, see the [main JExten README](../README.md).

## License

Apache License 2.0
