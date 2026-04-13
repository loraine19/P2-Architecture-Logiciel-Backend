package com.openclassrooms.etudiant.messages;

/**
 * @file UserErrorMessage.java
 *       Error and system failure messages.
 */
public enum UserErrorMessage {
    /* VALIDATION */
    DATA_NULL("User data cannot be null"),
    LOGIN_EMPTY("User login cannot be empty"),
    PASSWORD_EMPTY("Password cannot be empty"),
    LOGIN_EXISTS("Login already exists"),

    /* AUTH & SECURITY */
    INVALID_CREDENTIALS("Invalid login credentials"),
    REGISTRATION_FAILED("User registration failed"),
    TOKEN_INVALID("Token not valid"),
    TOKEN_REFRESH_FAILED("Token refresh failed"),
    REFRESH_TOKEN_DB_MISSING("No refresh token in DB"),
    REFRESH_TOKEN_MISMATCH("Refresh token hash mismatch"),

    /* SYSTEM & TECHNICAL */
    USER_NOT_FOUND("User not found"),
    JVM_ALGO_MISSING("SHA-256 is mandatory in any JVM");

    private final String message;

    UserErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}