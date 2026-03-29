package com.openclassrooms.etudiant.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response entity
 * Contains authentication status and JWT token
 * Used for login/register API responses
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Auth {

    // Authentication status - true if user is successfully authenticated
    @NotNull(message = "Authentication status is required")
    private Boolean isAuthenticated;

    // JWT token - full token for client usage (logged partially for security)
    @NotBlank(message = "Token is required")
    private String token;

    /**
     * Get partial token for logging purposes (security)
     * Shows only first 5 characters to avoid token exposure in logs
     */
    public String getPartialToken() {
        if (token == null || token.isEmpty()) {
            return "[empty]";
        }
        return token.length() > 5 ? token.substring(0, 5) + "..." : token;
    }
}
