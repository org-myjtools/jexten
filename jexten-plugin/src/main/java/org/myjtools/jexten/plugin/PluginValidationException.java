package org.myjtools.jexten.plugin;

import java.util.List;

/**
 * Exception thrown when a plugin fails validation before installation.
 * <p>
 * This exception contains the plugin ID that failed validation and all
 * validation errors encountered.
 *
 * @see PluginValidator
 * @see ValidationResult
 */
public class PluginValidationException extends PluginException {

    private final PluginID pluginId;
    private final List<String> validationErrors;


    /**
     * Creates a new validation exception.
     *
     * @param pluginId the ID of the plugin that failed validation
     * @param errors the list of validation error messages
     */
    public PluginValidationException(PluginID pluginId, List<String> errors) {
        super("Plugin {} failed validation: {}", pluginId, String.join("; ", errors));
        this.pluginId = pluginId;
        this.validationErrors = List.copyOf(errors);
    }


    /**
     * Returns the ID of the plugin that failed validation.
     *
     * @return the plugin ID
     */
    public PluginID pluginId() {
        return pluginId;
    }


    /**
     * Returns the list of validation errors.
     *
     * @return an unmodifiable list of error messages
     */
    public List<String> validationErrors() {
        return validationErrors;
    }
}
