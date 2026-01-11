# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

## [Unreleased]

### Planned
- Gradle plugin support
- Additional scopes for extension lifecycle
- Plugin dependency resolution improvements
