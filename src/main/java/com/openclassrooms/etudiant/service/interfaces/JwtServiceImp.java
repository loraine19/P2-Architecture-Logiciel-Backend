package com.openclassrooms.etudiant.service.interfaces;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT service contract for token generation, validation, and extraction.
 */
public interface JwtServiceImp {
    /* ACCESS */
    String generateToken(UserDetails userDetails, Boolean isRefresh);

    String getJwtFromCookies(HttpServletRequest request, Boolean isRefresh);

    String getJwtFromHeader(HttpServletRequest request);

    /* VALIDATION */
    String extractUsername(String token, Boolean isRefresh);

    boolean isTokenValid(String token, UserDetails userDetails, Boolean isRefresh);
}
