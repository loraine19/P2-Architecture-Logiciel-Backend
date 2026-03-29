package com.openclassrooms.etudiant.configuration.logging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Configuration for HTTP request logging (DEBUG/MONITORING tool)
 * Logs incoming HTTP requests to help debug API calls
 * 
 * AUTO CONFIG based on ENV variable:
 * - ENV=prod: headers=false, payload=false (security first)
 * - ENV=staging: headers=true, payload=false (auth debug, no data)
 * - ENV=dev: headers=true, payload=true (full debug)
 *
 * This is SEPARATE from mobile/web auth - it's just for monitoring
 */
@Configuration
public class RequestLoggingFilterConfig {

    // Auto-determine logging config based on environment
    @Value("${ENV:dev}")
    private String environment;

    @Value("${app.logging.requests.enabled:true}")
    private boolean requestLoggingEnabled;

    @Value("${app.logging.requests.max-payload-length:1000}")
    private int maxPayloadLength;

    @Bean
    public CommonsRequestLoggingFilter commonsRequestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

        // Auto-configure based on environment
        boolean includeHeaders = determineIncludeHeaders();
        boolean includePayload = determineIncludePayload();

        // Basic request info (always safe to log)
        filter.setIncludeQueryString(true);
        filter.setIncludeClientInfo(true);

        // Environment-based configuration
        filter.setIncludePayload(includePayload);
        filter.setIncludeHeaders(includeHeaders);
        filter.setMaxPayloadLength(maxPayloadLength);

        // Custom prefix with environment info
        filter.setAfterMessagePrefix("[" + environment.toUpperCase() + "] HTTP_REQUEST: ");
        filter.setBeforeMessagePrefix("[" + environment.toUpperCase() + "] HTTP_REQUEST_START: ");

        return filter;
    }

    /**
     * Determine if headers should be logged based on environment
     * prod=false (security), staging/dev=true (debug)
     */
    private boolean determineIncludeHeaders() {
        return !"prod".equalsIgnoreCase(environment);
    }

    /**
     * Determine if payload should be logged based on environment
     * prod/staging=false (security), dev=true (full debug)
     */
    private boolean determineIncludePayload() {
        return "dev".equalsIgnoreCase(environment);
    }
}
