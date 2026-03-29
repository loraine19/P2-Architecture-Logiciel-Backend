package com.openclassrooms.etudiant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User profile information returned after successful login
 * Contains only safe data for front-end display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String login;
    private String firstName;
    private String lastName;

    // Note: password is NEVER included for security
}