package com.openclassrooms.etudiant.dto;

import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user login requests
 * Contains login credentials with validation
 */
@Data
public class LoginRequestDTO {

    @NotBlank(message = "Login is required")
    @Size(min = 3, max = 100, message = "Login must be between 3 and 100 characters")
    @Email(message = "Login must be a valid email address")
    private String login;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    private boolean rememberMe;

    private AuthType authType = AuthType.COOKIE;

}
