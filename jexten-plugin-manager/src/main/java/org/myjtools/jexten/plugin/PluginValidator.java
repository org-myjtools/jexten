package org.myjtools.jexten.plugin;

/**
 * Interface for custom plugin validators.
 * <p>
 * Implementations can perform custom validation logic on plugin manifests
 * before they are installed. Validators are executed in the order they are
 * added to the {@link PluginManager}.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a custom validator
 * PluginValidator licenseValidator = manifest -> {
 *     if (manifest.licenseName() == null || manifest.licenseName().isBlank()) {
 *         return ValidationResult.invalid("Plugin must specify a license");
 *     }
 *     return ValidationResult.valid();
 * };
 *
 * // Add to plugin manager
 * pluginManager.addValidator(licenseValidator);
 * }</pre>
 *
 * <h2>Built-in Validators</h2>
 * The framework provides several built-in validators through static factory methods:
 * <ul>
 *   <li>{@link #requireLicense()} - Ensures plugins have a license specified</li>
 *   <li>{@link #requireUrl()} - Ensures plugins have a URL specified</li>
 *   <li>{@link #requireDescription()} - Ensures plugins have a description</li>
 * </ul>
 *
 * @see ValidationResult
 * @see PluginManager#addValidator(PluginValidator)
 */
@FunctionalInterface
public interface PluginValidator {

    /**
     * Validates a plugin manifest.
     *
     * @param manifest the plugin manifest to validate
     * @return the validation result indicating success or failure with error messages
     */
    ValidationResult validate(PluginManifest manifest);


    /**
     * Creates a composite validator that runs this validator followed by another.
     * <p>
     * Both validators are always executed, and their results are merged.
     *
     * @param other the other validator to chain
     * @return a new validator that combines both validations
     */
    default PluginValidator and(PluginValidator other) {
        return manifest -> this.validate(manifest).merge(other.validate(manifest));
    }


    /**
     * Creates a validator that requires plugins to have a license specified.
     *
     * @return a validator that checks for license presence
     */
    static PluginValidator requireLicense() {
        return manifest -> {
            if (manifest.licenseName() == null || manifest.licenseName().isBlank()) {
                return ValidationResult.invalid("Plugin must specify a license (licenseName)");
            }
            return ValidationResult.valid();
        };
    }


    /**
     * Creates a validator that requires plugins to have a URL specified.
     *
     * @return a validator that checks for URL presence
     */
    static PluginValidator requireUrl() {
        return manifest -> {
            if (manifest.url() == null || manifest.url().isBlank()) {
                return ValidationResult.invalid("Plugin must specify a URL");
            }
            return ValidationResult.valid();
        };
    }


    /**
     * Creates a validator that requires plugins to have a description.
     *
     * @return a validator that checks for description presence
     */
    static PluginValidator requireDescription() {
        return manifest -> {
            if (manifest.description() == null || manifest.description().isBlank()) {
                return ValidationResult.invalid("Plugin must have a description");
            }
            return ValidationResult.valid();
        };
    }


    /**
     * Creates a validator that requires plugins to have a display name.
     *
     * @return a validator that checks for display name presence
     */
    static PluginValidator requireDisplayName() {
        return manifest -> {
            if (manifest.displayName() == null || manifest.displayName().isBlank()) {
                return ValidationResult.invalid("Plugin must have a display name");
            }
            return ValidationResult.valid();
        };
    }


    /**
     * Creates a validator that checks if the plugin version is at least the specified minimum.
     *
     * @param minVersion the minimum required version (e.g., "1.0.0")
     * @return a validator that checks the minimum version requirement
     */
    static PluginValidator requireMinimumVersion(String minVersion) {
        return manifest -> {
            try {
                var required = org.myjtools.jexten.Version.of(minVersion);
                var actual = manifest.version();
                if (actual.compareTo(required) < 0) {
                    return ValidationResult.invalid(
                        "Plugin version " + actual + " is below minimum required " + minVersion
                    );
                }
                return ValidationResult.valid();
            } catch (IllegalArgumentException e) {
                return ValidationResult.invalid("Invalid minimum version format: " + minVersion);
            }
        };
    }
}
