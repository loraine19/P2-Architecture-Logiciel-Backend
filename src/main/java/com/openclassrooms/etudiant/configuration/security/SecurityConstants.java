package com.openclassrooms.etudiant.configuration.security;

import java.util.Arrays;
import java.util.List;

/**
 * Defines public endpoints that bypass JWT authentication.
 * Used by both the security filter chain and the JWT filter.
 */
public final class SecurityConstants {

    static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/register",
            "/api/login",
            "/api/logout",
            "/api/refresh",
            "/actuator/**",
            "/error",
            "/",
            "/index.html");

    public static boolean isPublicEndpoint(String requestURI) {
        return PUBLIC_PATHS.stream().anyMatch(path -> {
            if (path.endsWith("/**")) {
                return requestURI.startsWith(path.substring(0, path.length() - 3));
            }
            return requestURI.equals(path);
        });
    }

    private SecurityConstants() {
        throw new IllegalStateException("Utility class");
    }
}