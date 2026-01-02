# Java Plugin & Modularity Framework Comparison
## JExten vs PF4J vs OSGi vs Layrry

This document compares four approaches to extensibility and modularity in Java:
**JExten**, **PF4J**, **OSGi**, and **Layrry**, with a focus on plugin architectures, runtime behavior, and developer experience.

---

## 1. JExten
https://github.com/org-myjtools/jexten
### Overview
**JExten** is a **modern, JPMS-native extension and plugin framework** designed for Java 21+.
It combines the simplicity of annotation-driven extension points with **strong module isolation**, **semantic versioning**, and **built-in dependency injection**, without requiring an external container.

Unlike classic plugin frameworks, JExten is built directly on top of the **Java Platform Module System (JPMS)** and `ModuleLayer`s.

### Core Concepts
- Extension points declared with `@ExtensionPoint`
- Implementations annotated with `@Extension`
- JPMS modules as the fundamental unit
- Plugins loaded into dedicated `ModuleLayer`s
- Semantic version compatibility checks
- Deterministic resolution via priorities
- Built-in dependency injection (`@Inject`)
- Scoped lifecycles (SINGLETON, LOCAL, TRANSIENT)

### Key Strengths
- **JPMS-native architecture** (no custom classloader tricks)
- **Dynamic plugin loading** without restarting the JVM
- **Built-in dependency injection**, no Spring required
- **Semantic versioning enforcement** at runtime
- **Priority-based extension selection**
- **Low configuration overhead** (annotations + `plugin.yaml`)
- Clean, explicit architecture aligned with modern Java

### Weaknesses
- Requires Java 21+
- Newer ecosystem compared to OSGi or PF4J
- Opinionated extension model (less free-form than Layrry)

### Best Fit
- Modern Java applications targeting Java 21+
- Plugin-based systems with strong modular boundaries
- Teams wanting a **middle ground between PF4J simplicity and OSGi power**
- Projects that want to embrace JPMS instead of bypassing it

---

## 2. PF4J (Plugin Framework for Java)
https://pf4j.org/
### Overview
PF4J is a **lightweight, pragmatic plugin framework** focused on simplicity and ease of use.

It provides a clear plugin abstraction with minimal configuration and is widely used as a replacement for custom plugin systems.

### Core Concepts
- Plugins packaged as JARs
- Per-plugin `ClassLoader`
- Plugin lifecycle (`start`, `stop`)
- Extension points via interfaces
- Discovery via annotations or `ServiceLoader`

### Strengths
- Very easy to learn and adopt
- Clear plugin abstraction
- Minimal runtime overhead
- Good documentation and ecosystem
- Spring integration available

### Weaknesses
- No JPMS integration
- Limited dependency/version management
- No built-in dependency injection
- Classloader-based isolation only

### Best Fit
- Classic plugin architectures
- Tools and applications needing fast extensibility
- Projects prioritizing simplicity over strict modularity

---

## 3. OSGi (Apache Felix / Equinox)
https://www.osgi.org/
### Overview
OSGi is a **full-fledged dynamic module system** with over 20 years of production use.
It predates JPMS and provides extremely powerful modularity features.

### Core Concepts
- Bundles with explicit package exports/imports
- Dynamic service registry
- Version ranges and optional dependencies
- Runtime installation, update, and removal of bundles

### Strengths
- Fine-grained package-level isolation
- Advanced versioning and dependency resolution
- Mature ecosystem and tooling
- Proven in large enterprise platforms

### Weaknesses
- Very steep learning curve
- Heavy runtime and configuration
- Requires an OSGi container
- Not JPMS-native

### Best Fit
- Large, long-lived enterprise systems
- Platforms requiring complex version coexistence
- Organizations with existing OSGi expertise

---

## 4. Layrry
https://github.com/moditect/layrry
### Overview
Layrry is a **classloader and module-layer orchestration tool**, not a plugin framework.
It focuses on **dependency isolation and layering**, leaving extension models to the user.
However, its use of module layers makes it a modern choice for modular applications.

### Core Concepts
- Applications composed of layers
- Each layer mapped to a `ModuleLayer`
- YAML/TOML-based configuration
- Explicit dependency graphs

### Strengths
- Very explicit and predictable isolation model
- Excellent for avoiding dependency conflicts
- Works well with JPMS
- Minimal runtime API

### Weaknesses
- No plugin or extension abstraction
- No lifecycle or service model
- No dependency injection
- Requires significant custom architecture on top

### Best Fit
- Foundations for custom plugin systems
- Applications prioritizing isolation over ergonomics
- Teams wanting maximum architectural control

---

## Why JExten? — Architectural Comparison

| Capability | **JExten**                        | PF4J | OSGi | Layrry |
|----------|-----------------------------------|------|------|--------|
| **Built for modern Java** | ✅ **Yes (JPMS, Java 21+)**        | ❌ No | ❌ No | ⚠️ Partial |
| **Native JPMS integration** | ✅ **First-class (`ModuleLayer`)** | ❌ Bypasses JPMS | ❌ Parallel module system | ✅ Yes |
| **Plugin model out of the box** | ✅ **Yes**                         | ✅ Yes | ❌ Indirect | ❌ No |
| **Annotation-driven extensions** | ✅ **Yes**                         | ✅ Yes | ❌ No | ❌ No |
| **Dynamic loading without restart** | ✅ **Yes**                         | ✅ Yes | ✅ Yes | ✅ Yes |
| **Dependency isolation** | ✅ **Strong (JPMS boundaries)**    | ⚠️ Classloader-only | ✅ Package-level | ✅ Layer-level |
| **Semantic version compatibility** | ✅ **Built-in & enforced**         | ❌ None | ⚠️ Manual ranges | ❌ None |
| **Priority-based resolution** | ✅ **Deterministic by design**     | ❌ No | ⚠️ Service ranking | ❌ No |
| **Built-in dependency injection** | ✅ **Yes (no Spring needed)**      | ❌ No | ⚠️ DS required | ❌ No |
| **Scoped lifecycles** | ✅ **Yes**                         | ❌ No | ⚠️ Indirect | ❌ No |
| **Configuration verbosity** | ✅ **Minimal**                     | ✅ Minimal | ❌ Heavy | ⚠️ Medium |
| **Runtime footprint** | ✅ **Lightweight**                 | ✅ Lightweight | ❌ Heavy | ✅ Lightweight |
| **Learning curve** | ✅ **Low**                         | ✅ Low | ❌ Very high | ⚠️ Medium |
| **Opinionated plugin architecture** | ✅ **Yes (by design)**             | ✅ Yes | ❌ No | ❌ No |
| **Requires external container** | ❌ **No**                          | ❌ No | ✅ Yes | ❌ No |
| **Suitable as a plugin framework** | ✅ **Excellent**                   | ✅ Good | ⚠️ Complex | ❌ Poor |
| **Suitable as a foundation only** | ⚠️ Optional                       | ❌ No | ❌ No | ✅ Yes |

---

### Key Takeaways

- **PF4J** solves *plugins*, but **ignores JPMS** and offers no modern modular guarantees.
- **OSGi** solves *everything*, but at the cost of **complexity, heavy tooling, and a parallel universe** to modern Java.
- **Layrry** solves *isolation*, but **does not solve plugins at all**.
- **JExten** is a **solution** that:
    - Is **JPMS-native**
    - Provides a **complete plugin model**
    - Enforces **semantic version compatibility**
    - Includes **dependency injection and lifecycle management**
    - Keeps a **low learning curve**
