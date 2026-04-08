package com.openclassrooms.etudiant.enums;

/**
 * Successful operation messages.
 */
public enum UserMessage {
    /* AUTH */
    LOGIN_SUCCESS("Logged in successfully"),
    LOGOUT_SUCCESS("User logged out successfully"),
    REGISTER_SUCCESS("User registered successfully"),
    TOKEN_REFRESH_SUCCESS("Token refreshed successfully");

    private final String message;

    UserMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}