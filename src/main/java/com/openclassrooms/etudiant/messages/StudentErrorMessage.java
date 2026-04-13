package com.openclassrooms.etudiant.messages;

/**
 * Error and validation messages for student operations.
 */
public enum StudentErrorMessage {

    /* VALIDATION */
    ID_NULL("Student ID cannot be null"),
    DETAILS_NULL("Student details cannot be null"),
    STUDENT_NULL("Student cannot be null"),
    EMAIL_EMPTY("Student email cannot be empty"),

    /* NOT FOUND */
    NOT_FOUND_ID("Student not found with ID: %s"),
    NOT_FOUND_EMAIL("Student not found with email: %s"),
    DELETE_NOT_FOUND("Cannot delete: Student not found with ID: %s"),

    /* CONFLICT */
    EMAIL_EXISTS("Another student already exists with email: %s");

    private final String message;

    StudentErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}
