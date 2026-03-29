package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.service.interfaces.JwtServiceInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT token service for authentication and authorization
 * Handles token generation, validation, and extraction
 * Provides secure token management with proper error handling
 */
@Service
@Slf4j
public class JwtService implements JwtServiceInterface {

    // JWT Configuration from environment
    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${JWT_REFRESH_SECRET}")
    private String jwtRefreshSecret;

    @Value("${JWT_EXPIRATION_MS:3600000}") // Default: 1 hour
    private long jwtExpirationMs;

    @Value("${JWT_REFRESH_EXPIRATION_MS:172800000}") // Default: 48 hours
    private long jwtRefreshExpirationMs;

    /**
     * Get signing key for JWT operations
     * Creates HMAC key from base64 encoded secret
     */
    public SecretKey getSigningKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to create JWT signing key", e);
            throw new IllegalStateException("Invalid JWT secret configuration");
        }
    }

    private SecretKey getSigningKey() {
        return getSigningKey(jwtSecret);
    }

    private SecretKey getRefreshSigningKey() {
        return getSigningKey(jwtRefreshSecret);
    }

    /**
     * Generate JWT token for authenticated user
     */
    @Override
    public String generateToken(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("UserDetails cannot be null");
        }

        try {
            String token = Jwts.builder()
                    .subject(userDetails.getUsername())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(getSigningKey())
                    .compact();

            log.debug("JWT token generated for user: {}", userDetails.getUsername());
            return token;

        } catch (Exception e) {
            log.error("Failed to generate JWT token for user: {}", userDetails.getUsername(), e);
            throw new IllegalStateException("Token generation failed");
        }
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("UserDetails cannot be null");
        }

        try {
            String token = Jwts.builder()
                    .subject(userDetails.getUsername())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                    .signWith(getRefreshSigningKey())
                    .compact();

            log.debug("JWT refresh token generated for user: {}", userDetails.getUsername());
            return token;

        } catch (Exception e) {
            log.error("Failed to generate JWT refresh token for user: {}", userDetails.getUsername(), e);
            throw new IllegalStateException("Refresh token generation failed");
        }
    }

    /**
     * Extract all claims from JWT token with error handling
     */
    private Claims extractAllClaims(String token, SecretKey secretKey) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    /**
     * Extract username from JWT token
     */
    @Override
    public String extractUsername(String token, Boolean isRefresh) {
        try {
            String username = extractAllClaims(token, isRefresh ? getRefreshSigningKey() : getSigningKey())
                    .getSubject();
            log.debug("Username extracted from token: {}", username);
            return username;
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate JWT token against user details
     */
    @Override
    public boolean isTokenValid(String token, UserDetails userDetails, SecretKey secretKey) {
        if (userDetails == null) {
            return false;
        }

        try {
            final String username = extractUsername(token, secretKey.equals(getRefreshSigningKey()));
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token, secretKey);
            log.debug("Token validation result for user {}: {}", userDetails.getUsername(), isValid);
            return isValid;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    private boolean isTokenExpired(String token, SecretKey secretKey) {
        try {
            Date expiration = extractAllClaims(token, secretKey).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Failed to check token expiration: {}", e.getMessage());
            return true; // Consider expired if we can't check
        }
    }

    /**
     * Extract JWT token from HTTP cookies
     * Used for web application authentication
     */
    @Override
    public String getJwtFromCookies(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            Cookie cookie = WebUtils.getCookie(request, "token");
            if (cookie != null && cookie.getValue() != null && !cookie.getValue().trim().isEmpty()) {
                log.debug("JWT token found in cookies");
                return cookie.getValue();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract JWT from cookies: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getJwtRefreshFromCookies(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            Cookie cookie = WebUtils.getCookie(request, "refreshToken");
            if (cookie != null && cookie.getValue() != null && !cookie.getValue().trim().isEmpty()) {
                log.debug("JWT refresh token found in cookies");
                return cookie.getValue();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract JWT refresh token from cookies: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract JWT token from HTTP headers
     * Used for web application authentication via headers
     */
    @Override
    public String getJwtFromHeader(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                log.debug("JWT token found in Authorization header");
                return authHeader.substring(7); // Remove "Bearer " prefix
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract JWT from headers: {}", e.getMessage());
            return null;
        }
    }
}