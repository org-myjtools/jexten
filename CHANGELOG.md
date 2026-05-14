# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-14

### Added

- Stable release — all alpha features promoted to production quality.

## [1.0.0-alpha5] - 2026-05-10

### Added

- `PluginManager.moduleLayerTree()` — returns the `ModuleLayerTree` describing the current
  plugin module layer hierarchy, useful for diagnostics and debug output.

### Fixed

- `PluginManager.addRuntimeDependency()` now resolves the versioned artifact name when
  the caller passes a bare artifact ID without a version (e.g. `h2` instead of `h2-2.2.0`).
  The artifact is downloaded via the `ArtifactStore`, installed, and the versioned name is
  derived from the returned JAR filename before persisting the runtime config and reloading
  the plugin.
- Plugin installation now copies the plugin artifact and resolved dependencies before
  persisting the manifest, so module discovery sees a complete artifact set during
  install and reload operations.
- Plugin module layer resolution now filters out modules already provided by parent
  layers, avoiding "reads more than one module" resolution errors when automatic
  modules are visible both in the parent configuration and in plugin artifacts.
- Plugin module layer resolution now uses `ModuleFinder.ofSystem()` as the fallback finder,
  so plugins that transitively require non-default JDK modules (e.g. `jdk.xml.dom` required
  by `xmlbeans`) resolve correctly without needing those modules pre-loaded in the boot layer.

## [1.0.0-alpha1] - 2026-01-11

### Added

- Core annotations: `@ExtensionPoint`, `@Extension`, `@Inject`, `@PostConstruct`
- `ExtensionManager` for discovering and managing extensions via ServiceLoader
- `PluginManager` for dynamic plugin loading at runtime
- Annotation processor with compile-time validation for JPMS modules
- Maven plugin (`jexten-maven-plugin`) for generating plugin manifests and bundles
- Maven artifact store integration for dependency resolution
- Support for priority-based extension resolution (`Priority.HIGHEST` to `Priority.LOWEST`)
- Scoped extension instances: `SINGLETON`, `SESSION`, `TRANSIENT`
- Plugin hot-reload capability
- Plugin validation and error recovery
- Comprehensive test suites for all modules
- GitHub Actions CI/CD workflow

### Changed

- Renamed module `jexten-plugin` to `jexten-plugin-manager` for clarity

### Fixed

- Hot reload test stability
- Test classpath configuration for annotation processor tests

## [1.0.0-alpha4] - 2026-05-05

### Fixed

- `generate-manifest` goal now uses `project.getArtifacts()` (full resolved transitive tree)
  instead of `project.getDependencies()` (direct dependencies only). This fixes a
  `FindException` at runtime when a plugin dependency declares `requires` on a transitive
  module (e.g. `poi-ooxml` requiring `org.apache.logging.log4j`) that was absent from the
  generated manifest and therefore missing from the plugin module layer.
- Added `requiresDependencyResolution = COMPILE_PLUS_RUNTIME` to the `generate-manifest`
  Mojo so Maven resolves transitive artifacts before the goal executes.

## [1.0.0-alpha2] - 2026-04-18

### Added

- Runtime dependencies for plugins: a plugin can now have additional artifacts added to its
  module layer after installation, without modifying the plugin bundle itself.
  - `PluginManager.addRuntimeDependency(PluginID, group, artifact)` — registers a runtime dep,
    fetches it from the `ArtifactStore` if not already present, and reloads the plugin.
  - `PluginManager.removeRuntimeDependency(PluginID, group, artifact)` — removes a runtime dep
    and reloads the plugin.
  - `PluginManager.getRuntimeDependencies(PluginID)` — returns the current runtime deps map.
  - Runtime deps are persisted in a separate `<group>-<name>.runtime.yaml` file alongside the
    plugin manifest, so they survive application restarts and manager refreshes.
