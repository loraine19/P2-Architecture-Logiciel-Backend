package com.openclassrooms.etudiant.enums;

/**
 * Error messages for global exception handling.
 */
public enum ExceptionErrorMessage {

    /* VALIDATION */
    VALIDATION_FAILED("Validation failed for one or more fields"),
    INVALID_ARGUMENT_FALLBACK("Invalid argument provided"),

    /* RESOURCE */
    ENTITY_NOT_FOUND_FALLBACK("Requested entity not found"),
    RESOURCE_NOT_FOUND("The requested resource was not found: %s"),
    DUPLICATE_ENTRY("Resource already exists - duplicate entry detected"),
    DATA_INTEGRITY_ERROR("Database constraint violation"),

    /* AUTH */
    INVALID_CREDENTIALS("Invalid credentials provided"),
    ACCESS_DENIED("Insufficient permissions to access this resource"),

    /* HTTP */
    METHOD_NOT_SUPPORTED("HTTP method '%s' not supported for this endpoint"),
    TYPE_MISMATCH("Invalid value '%s' for parameter '%s'"),

    /* SERVER */
    RUNTIME_ERROR("An unexpected error occurred during request processing"),
    INTERNAL_ERROR("An internal server error occurred");

    private final String message;

    ExceptionErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}
