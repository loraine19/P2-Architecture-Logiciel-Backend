package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.micrometer.common.lang.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.SecretKey;

/**
 * JWT Authentication Filter - handles both cookies (web) and headers (mobile)
 * Processes JWT tokens for secured endpoints
 * Always accepts both cookie and header-based authentication
 * 
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Value("${ENV:dev}")
    private String env;

    @Value("${JWT_EXPIRATION_MS:3600000}")
    private Long jwtExpirationMs;

    @Value("${JWT_REFRESH_EXPIRATION_MS:172800000}")
    private Long jwtRefreshExpirationMs;

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${JWT_REFRESH_SECRET}")
    private String jwtRefreshSecret;

    private SecretKey getSigningKey() {
        return this.jwtService.getSigningKey(jwtSecret);
    }

    private SecretKey getRefreshSigningKey() {
        return this.jwtService.getSigningKey(jwtRefreshSecret);
    }

    // Angular frontend determines auth type
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip JWT processing for public endpoints (use centralized SecurityConstants)
        if (SecurityConstants.isPublicEndpoint(requestURI, "dev")) { // Always use dev mode for endpoint checking
            log.debug("Public endpoint accessed: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // Try to get JWT from cookies first (web app), then headers (mobile app)
        String jwt = getJwtFromRequest(request, response);
        if (jwt == null) {
            log.debug("No JWT token found in cookies or headers for: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // Validate token format
        if (jwt.trim().isEmpty() || !jwt.contains(".")) {
            log.warn("Malformed JWT token received from: {}", getClientIP(request));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Malformed JWT token");
            return;
        }

        try {
            // Extract username from token
            String userLogin = jwtService.extractUsername(jwt, false);

            // Check if user is not already authenticated
            if (userLogin != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userLogin);

                // Validate token and create auth context
                if (jwtService.isTokenValid(jwt, userDetails, getSigningKey())) {

                    // Create authentication object
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // password not stored for security
                            userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("User authenticated with authorities: {}", userDetails.getAuthorities());
                } else {
                    log.warn("Invalid or expired JWT token for user: {} from: {}", userLogin, getClientIP(request));
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("Invalid token");
                    return;
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for user: {} from: {}",
                    e.getClaims().getSubject(), getClientIP(request));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Token expired");
            return;
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token from IP: {} - potential attack", getClientIP(request));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Malformed token");
            return;
        } catch (SignatureException e) {
            log.error("JWT signature validation failed from IP: {} - SECURITY BREACH ATTEMPT", getClientIP(request));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid token signature");
            return;
        } catch (Exception e) {
            log.error("CRITICAL: JWT processing error - investigate immediately! Error: {} from IP: {}",
                    e.getMessage(), getClientIP(request), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Authentication service error");
            return;
        }

        // Continue to next filter in chain
        filterChain.doFilter(request, response);
    }

    /**
     * Get JWT token from request cookies or headers based on environment
     */
    private String getJwtFromRequest(HttpServletRequest request, HttpServletResponse response) {
        // Priority 1: Always check cookies (web app standard)
        String jwtFromCookies = jwtService.getJwtFromCookies(request);
        if (jwtFromCookies != null) {
            log.debug("JWT found in cookies");
            return jwtFromCookies;
        }

        // Priority 2: Always check Authorization header (Angular decides auth type)
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Debug log for header content
            log.debug("JWT found in Authorization header");
            return authHeader.substring(7); // Remove "Bearer " prefix
        }

        // Priority 3: Handle refresh token and generate new access token
        return handleRefreshTokenFlow(request, response);
    }

    /**
     * Handle refresh token flow and generate new access token
     * Follows Single Responsibility Principle by isolating refresh token logic
     */
    private String handleRefreshTokenFlow(HttpServletRequest request, HttpServletResponse response) {
        String jwtRefreshFromCookies = jwtService.getJwtRefreshFromCookies(request);
        if (jwtRefreshFromCookies == null) {
            return null;
        }

        if (!validateRefreshToken(jwtRefreshFromCookies)) {
            return null;
        }

        return generateAndSetNewAccessToken(jwtRefreshFromCookies, response);
    }

    /**
     * Generate new access token and update cookie
     * Respects Open/Closed Principle - can be extended without modification
     */
    public String generateAndSetNewAccessToken(String refreshToken, @Nullable HttpServletResponse response) {
        try {
            String userLogin = jwtService.extractUsername(refreshToken, true);
            Optional<User> userOptional = userRepository.findByLogin(userLogin);

            if (userOptional.isEmpty()) {
                log.warn("User not found during token refresh: {}", userLogin);
                return null;
            }

            User user = userOptional.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getLogin());
            String newAccessToken = jwtService.generateToken(userDetails);

            // Update access token cookie (following dependency inversion principle)
            if (response != null)
                updateAccessTokenCookie(response, newAccessToken);

            log.debug("New access token generated and cookie updated for user: {}", userLogin);
            return newAccessToken;

        } catch (Exception e) {
            log.error("Error during token refresh: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Update access token cookie with new token
     * Implements Interface Segregation - focused single responsibility
     */
    private void updateAccessTokenCookie(HttpServletResponse response, String newAccessToken) {
        @SuppressWarnings("null")
        ResponseCookie accessTokenCookie = ResponseCookie.from("token", newAccessToken)
                .httpOnly(true)
                .secure("prod".equalsIgnoreCase(env))
                .path("/")
                .maxAge(jwtExpirationMs / 1000) // Convert to seconds
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        log.debug("Access token cookie updated successfully");
    }

    private String getClientIP(HttpServletRequest request) {
        // Check proxy headers first
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        // Fall back to standard remote address
        return request.getRemoteAddr();
    }

    /**
     * Validate refresh token against database
     */
    public boolean validateRefreshToken(String refreshToken) {
        ; // Call service method to validate token against database
        try {
            // Extract username from refresh token
            String userLogin = jwtService.extractUsername(refreshToken, true);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userLogin);
            Boolean isValid = jwtService.isTokenValid(refreshToken, userDetails, getRefreshSigningKey());
            if (userLogin == null || userDetails == null || !isValid) {
                log.warn("No user login found in refresh token");
                return false;
            }

            // Find user in database
            Optional<User> userOptional = userRepository.findByLogin(userLogin);
            if (userOptional.isEmpty()) {
                log.warn("User not found for refresh token validation: {}", userLogin);
                return false;
            }

            User user = userOptional.get();
            String storedHashedRefreshToken = user.getRefreshToken();

            // If no refresh token stored in database, validation fails
            if (storedHashedRefreshToken == null || storedHashedRefreshToken.isEmpty()) {
                log.warn("No refresh token stored in database for user: {}", userLogin);
                return false;
            }

            // Hash the incoming refresh token and compare with stored hash
            String incomingTokenHash = hashWithSHA256(refreshToken);
            boolean isValidDB = storedHashedRefreshToken.equals(incomingTokenHash);

            if (isValidDB) {
                log.debug("Refresh token validated successfully for user: {}", userLogin);
            } else {
                log.warn("Refresh token hash mismatch for user: {}", userLogin);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating refresh token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Hash refresh token with SHA-256 (same method as UserService)
     */
    private String hashWithSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }
}