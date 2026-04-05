package com.openclassrooms.etudiant.configuration.security;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConstants class defines public endpoints and utility methods for
 * security configuration
 */
public final class SecurityConstants {

    // Public endpoints
    static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/register",
            "/api/login",
            "/api/logout",
            "/api/refresh",
            "/actuator/**",
            "/error",
            "/",
            "/index.html");

    // Verification endpoint
    public static boolean isPublicEndpoint(String requestURI) {
        return PUBLIC_PATHS.stream().anyMatch(path -> {
            // If the path ends with /**, check if the request URI starts with the base path
            if (path.endsWith("/**")) {
                return requestURI.startsWith(path.substring(0, path.length() - 3));
            }
            // Exact match
            return requestURI.equals(path);
        });
    }

    private SecurityConstants() {
        throw new IllegalStateException("Utility class");
    }
}