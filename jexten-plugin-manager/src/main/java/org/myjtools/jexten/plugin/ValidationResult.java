package org.myjtools.jexten.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of validating a plugin manifest.
 * <p>
 * A validation result can be either valid (no errors) or invalid (one or more errors).
 * Multiple validation results can be combined using {@link #merge(ValidationResult)}.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a valid result
 * ValidationResult valid = ValidationResult.valid();
 *
 * // Create an invalid result with errors
 * ValidationResult invalid = ValidationResult.invalid("Missing required field");
 *
 * // Create with multiple errors
 * ValidationResult errors = ValidationResult.invalid(List.of(
 *     "Invalid version format",
 *     "Unknown extension point"
 * ));
 *
 * // Combine results
 * ValidationResult combined = result1.merge(result2);
 * }</pre>
 */
public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(Collections.emptyList());

    private final List<String> errors;


    private ValidationResult(List<String> errors) {
        this.errors = List.copyOf(errors);
    }


    /**
     * Creates a valid result with no errors.
     *
     * @return a valid ValidationResult
     */
    public static ValidationResult valid() {
        return VALID;
    }


    /**
     * Creates an invalid result with a single error message.
     *
     * @param error the error message
     * @return an invalid ValidationResult
     */
    public static ValidationResult invalid(String error) {
        return new ValidationResult(List.of(error));
    }


    /**
     * Creates an invalid result with multiple error messages.
     *
     * @param errors the list of error messages
     * @return an invalid ValidationResult, or valid if the list is empty
     */
    public static ValidationResult invalid(List<String> errors) {
        if (errors.isEmpty()) {
            return VALID;
        }
        return new ValidationResult(errors);
    }


    /**
     * Returns whether this result represents a valid state (no errors).
     *
     * @return true if valid, false if there are errors
     */
    public boolean isValid() {
        return errors.isEmpty();
    }


    /**
     * Returns the list of error messages.
     *
     * @return an unmodifiable list of error messages (empty if valid)
     */
    public List<String> errors() {
        return errors;
    }


    /**
     * Merges this result with another, combining all errors.
     * <p>
     * If both results are valid, returns a valid result.
     * If either or both have errors, returns an invalid result with all errors combined.
     *
     * @param other the other validation result to merge
     * @return a new ValidationResult containing errors from both results
     */
    public ValidationResult merge(ValidationResult other) {
        if (this.isValid() && other.isValid()) {
            return VALID;
        }
        if (this.isValid()) {
            return other;
        }
        if (other.isValid()) {
            return this;
        }
        List<String> combined = new ArrayList<>(this.errors.size() + other.errors.size());
        combined.addAll(this.errors);
        combined.addAll(other.errors);
        return new ValidationResult(combined);
    }


    @Override
    public String toString() {
        if (isValid()) {
            return "ValidationResult[valid]";
        }
        return "ValidationResult[invalid: " + String.join("; ", errors) + "]";
    }
}
