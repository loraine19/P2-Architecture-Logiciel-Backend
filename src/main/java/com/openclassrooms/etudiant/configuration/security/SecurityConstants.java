package com.openclassrooms.etudiant.configuration.security;

/**
 * Security constants for endpoint configurations
 * Centralized public endpoint definitions for SpringSecurityConfig and
 * JwtAuthenticationFilter
 */
public final class SecurityConstants {

    /**
     * Public endpoints for development and staging environments
     */
    public static final String[] DEV_PUBLIC_ENDPOINTS = {
            "/api/register",
            "/api/login",
            "/api/logout",
            "/api/refresh",
            "/register",
            "/login",
            "/logout",
            "/refresh",
    };

    /**
     * Public endpoints for production environment
     */
    public static final String[] PROD_PUBLIC_ENDPOINTS = {
            "/api/register",
            "/api/login",
            "/api/logout",
            "/api/refresh",
            "/register",
            "/login",
            "/logout",
            "/refresh"
    };

    /**
     * Always public endpoints for all environments
     */
    public static final String[] ALWAYS_PUBLIC_ENDPOINTS = {
            "/actuator/**",
            "/error"
    };

    /**
     * Get public endpoints for environment
     */

    public static String[] getPublicEndpoints(String environment) {
        if ("prod".equalsIgnoreCase(environment)) {
            return combineEndpoints(PROD_PUBLIC_ENDPOINTS, ALWAYS_PUBLIC_ENDPOINTS);
        } else {
            return combineEndpoints(DEV_PUBLIC_ENDPOINTS, ALWAYS_PUBLIC_ENDPOINTS);
        }
    }

    /**
     * Check if URI is a public endpoint
     */
    public static boolean isPublicEndpoint(String requestURI, String environment) {
        String[] publicEndpoints = getPublicEndpoints(environment);

        for (String endpoint : publicEndpoints) {
            // Handle wildcard patterns like /actuator/**
            if (endpoint.endsWith("/**")) {
                String basePattern = endpoint.substring(0, endpoint.length() - 3);
                if (requestURI.startsWith(basePattern)) {
                    return true;
                }
            }
            // Exact match
            else if (requestURI.equals(endpoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Combine endpoint arrays
     */
    private static String[] combineEndpoints(String[] primary, String[] always) {
        String[] combined = new String[primary.length + always.length];
        System.arraycopy(primary, 0, combined, 0, primary.length);
        System.arraycopy(always, 0, combined, primary.length, always.length);
        return combined;
    }

    // Private constructor to prevent instantiation
    private SecurityConstants() {
        throw new IllegalStateException("Utility class - should not be instantiated");
    }
}