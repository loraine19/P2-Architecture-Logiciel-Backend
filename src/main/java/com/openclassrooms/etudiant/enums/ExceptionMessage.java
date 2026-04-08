package com.openclassrooms.etudiant.enums;

public enum ExceptionMessage {

    VALIDATION_FAILED("Validation failed for one or more fields"),
    DUPLICATE_ENTRY("Resource already exists - duplicate entry detected"),
    DATA_INTEGRITY_ERROR("Database constraint violation"),
    ENTITY_NOT_FOUND_FALLBACK("Requested entity not found"),
    INVALID_CREDENTIALS("Invalid credentials provided"),
    ACCESS_DENIED("Insufficient permissions to access this resource"),
    METHOD_NOT_SUPPORTED("HTTP method '%s' not supported for this endpoint"),
    RESOURCE_NOT_FOUND("The requested resource was not found: %s"),
    TYPE_MISMATCH("Invalid value '%s' for parameter '%s'"),
    INVALID_ARGUMENT_FALLBACK("Invalid argument provided"),
    RUNTIME_ERROR("An unexpected error occurred during request processing"),
    INTERNAL_ERROR("An internal server error occurred");

    private final String message;

    ExceptionMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}
