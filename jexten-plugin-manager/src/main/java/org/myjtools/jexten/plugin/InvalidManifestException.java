package org.myjtools.jexten.plugin;

import java.util.List;

/**
 * Exception thrown when a plugin manifest fails validation.
 * <p>
 * This exception contains a list of all validation errors found in the manifest,
 * allowing callers to report all issues at once rather than failing on the first error.
 */
public class InvalidManifestException extends RuntimeException {

    private final List<String> validationErrors;


    /**
     * Creates a new exception with the specified validation errors.
     *
     * @param errors the list of validation error messages
     */
    public InvalidManifestException(List<String> errors) {
        super("Invalid plugin manifest: " + String.join("; ", errors));
        this.validationErrors = List.copyOf(errors);
    }


    /**
     * Returns the list of validation errors found in the manifest.
     *
     * @return an unmodifiable list of error messages
     */
    public List<String> validationErrors() {
        return validationErrors;
    }
}
