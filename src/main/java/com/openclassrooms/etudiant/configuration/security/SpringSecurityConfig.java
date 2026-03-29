package com.openclassrooms.etudiant.configuration.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main security configuration for the application
 * Handles authentication, authorization and filtering
 * 
 * ENVIRONMENT-BASED CONFIG:
 * - ENV=dev: uses /api/* endpoints (for development with API prefix)
 * - ENV=prod: uses /* endpoints (clean URLs for production)
 * 
 * WHY ENV-based endpoints?
 * - Development: /api prefix helps distinguish API calls during dev
 * - Production: cleaner URLs without /api prefix for end users
 * - Security: centralized config with other sensitive settings in .env
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SpringSecurityConfig {

    private final CustomUserDetailService customUserDetailService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Auto-configure endpoints based on environment
    @Value("${ENV:dev}")
    private String environment;

    /**
     * Configure the authentication provider - MODERN SPRING SECURITY 6+ APPROACH
     * Uses new non-deprecated constructor with UserDetailsService
     * Follows Spring Security 6+ recommendations exactly
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        log.debug("Configuring modern DaoAuthenticationProvider for environment: {}", environment);

        // NEW Spring Security 6+ approach: UserDetailsService in constructor
        // This is the RECOMMENDED way to avoid all deprecation warnings
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailService);
        // Then set password encoder (this method is NOT deprecated)
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * Expose the authentication manager for the app
     * Used by login controller to authenticate users
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Define the password hashing algorithm
     * BCrypt is secure and industry standard
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Build the main security filter chain
     * Configures endpoints and security policies based on environment
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Get environment-specific endpoint patterns from centralized constants
        String[] publicEndpoints = SecurityConstants.getPublicEndpoints(environment);
        log.info("Configuring security for {} environment with endpoints: {}",
                environment, java.util.Arrays.toString(publicEndpoints));

        http
                // Disable features not needed for stateless APIs
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)

                // Set session policy to stateless (JWT-based, no server sessions)
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Modern authentication configuration
                .userDetailsService(customUserDetailService)

                // Link our custom authentication provider (configured with passwordEncoder)
                .authenticationProvider(authenticationProvider())

                // Define which routes are public or protected (from SecurityConstants)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicEndpoints).permitAll() // Centralized endpoint config
                        .anyRequest().authenticated()) // All other endpoints need auth

                // Plug our JWT filter before the standard login filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Handle unauthorized access errors with clear messages
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(
                        (request, response, exception) -> {
                            log.warn("Unauthorized access attempt to: {} from: {}",
                                    request.getRequestURI(), request.getRemoteAddr());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                        }));

        return http.build();
    }

}