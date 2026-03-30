package com.openclassrooms.etudiant.service.interfaces;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT Service Interface
 * Defines contract for JWT token management operations
 * 
 * This interface provides methods for:
 * - Token generation (access and refresh tokens)
 * - Token validation and verification
 * - Token extraction from requests
 * - User authentication through JWT
 * 
 * @author OpenClassrooms
 * @version 1.0
 */
public interface JwtServiceInterface {
    /* ACCESS */
    String generateToken(UserDetails userDetails, Boolean isRefresh);

    String getJwtFromCookies(HttpServletRequest request, Boolean isRefresh);

    String getJwtFromHeader(HttpServletRequest request);

    /* VALIDATION */
    String extractUsername(String token, Boolean isRefresh);

    boolean isTokenValid(String token, UserDetails userDetails, Boolean isRefresh);
}
