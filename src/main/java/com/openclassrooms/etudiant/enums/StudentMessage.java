package com.openclassrooms.etudiant.enums;

public enum StudentMessage {

    ID_CANNOT_BE_NULL("Student ID cannot be null"),
    NOT_FOUND_BY_ID("Student not found with ID: %s"),
    EMAIL_CANNOT_BE_EMPTY("Email cannot be empty"),
    NOT_FOUND_BY_EMAIL("Student not found with email: %s"),
    CANNOT_BE_NULL("Student cannot be null"),
    EMAIL_FIELD_CANNOT_BE_EMPTY("Student email cannot be empty"),
    EMAIL_ALREADY_EXISTS("Another student already exists with email: %s"),
    DETAILS_CANNOT_BE_NULL("Student details cannot be null"),
    DELETE_NOT_FOUND("Cannot delete: Student not found with ID: %s");

    private final String message;

    StudentMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}
