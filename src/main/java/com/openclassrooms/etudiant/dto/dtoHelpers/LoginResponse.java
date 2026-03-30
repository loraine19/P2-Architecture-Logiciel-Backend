package com.openclassrooms.etudiant.dto.dtoHelpers;

import com.openclassrooms.etudiant.dto.UserProfileDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response containing success message and user profile
 * Returned after successful authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String message;
    private boolean success;
    private UserProfileDTO user;
    private AuthType authType;
    private String refreshToken;

    public static LoginResponse success(String message, UserProfileDTO user, AuthType authType) {
        return new LoginResponse(message, true, user, authType, null);
    }

    public static LoginResponse success(String message, UserProfileDTO user, AuthType authType, String refreshToken) {
        return new LoginResponse(message, true, user, authType, refreshToken);
    }

    public static LoginResponse error(String message) {
        return new LoginResponse(message, false, null, null, null);
    }
}