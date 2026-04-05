package com.openclassrooms.etudiant.configuration.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Spring Security configuration class
 * Configures authentication provider, password encoding, and security filter
 * chain
 * Defines public endpoints and JWT authentication filter
 */

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SpringSecurityConfig {

    private final CustomUserDetailService customUserDetailService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /* AUTHENTICATION PROVIDER */
    @SuppressWarnings("deprecation")
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /* AUTHENTICATION MANAGER */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /* PASSWORD ENCODER */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* SECURITY FILTER CHAIN */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // On utilise directement la liste statique de SecurityConstants
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_PATHS.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated())

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, exception) -> {
                            log.warn("Accès refusé sur {} : {}", request.getRequestURI(), exception.getMessage());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                        }));

        return http.build();
    }
}