package com.openclassrooms.etudiant.service.interfaces;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;

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

    /**
     * Generate access JWT token for authenticated user
     * 
     * @param userDetails the authenticated user details
     * @return JWT access token string
     * @throws IllegalArgumentException if userDetails is null or invalid
     * @throws IllegalStateException    if token generation fails
     */
    String generateToken(UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);

    String extractUsername(String token, Boolean isRefresh);

    boolean isTokenValid(String token, UserDetails userDetails, SecretKey secretKey);

    String getJwtFromCookies(HttpServletRequest request);

    /**
     * Extract JWT token from HTTP headers
     * Used for web application authentication via headers
     * 
     * @param request the HTTP servlet request containing headers
     * @return JWT token from headers, or null if not found
     */
    String getJwtFromHeader(HttpServletRequest request);

    String getJwtRefreshFromCookies(HttpServletRequest request);
}
