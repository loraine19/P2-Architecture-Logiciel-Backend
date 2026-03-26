package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        System.out.println("🔐 JWT Filter: Processing request for " + requestURI);

        // Skip JWT processing for public endpoints
        if (isPublicEndpoint(requestURI)) {
            System.out.println("🔓 JWT Filter: Public endpoint detected - skipping JWT processing");
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = jwtService.getJwtFromCookies(request);
        if (jwt == null) {
            System.out.println("❌ JWT Filter: No JWT token found in cookies");
            filterChain.doFilter(request, response);
            return;
        }

        // Additional safety check for malformed tokens
        if (jwt.isEmpty() || !jwt.contains(".")) {
            System.out.println("❌ JWT Filter: Malformed or empty JWT token found");
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Check if header is missing or wrong format
         * final String authHeader = request.getHeader("Authorization");
         * if (authHeader == null || !authHeader.startsWith("Bearer ")) {
         * if (request.getRequestURI().equals("/api/login"))
         * System.out.
         * println("🔐 JWT Filter: Login endpoint accessed - skipping authentication");
         * else
         * System.out.
         * println("❌ JWT Filter: No valid Authorization header found - skipping authentication"
         * );
         * filterChain.doFilter(request, response);
         * return;
         * }
         */

        /* Check if user is not already authenticated */
        String userLogin = jwtService.extractUsername(jwt);
        System.out.println("🔍 JWT Filter: Extracted username from token: " + userLogin);
        if (userLogin != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("🔍 JWT Filter: User not authenticated, loading user details...");

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userLogin);
            System.out.println("✅ JWT Filter: User details loaded for: " + userDetails.getUsername());

            /* Check token validity in DB & update security context */
            if (jwtService.isTokenValid(jwt, userDetails)) {
                System.out.println("✅ JWT Filter: Token is valid, creating authentication...");
                /* Create authentication badge */
                var authBadge = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, /* password not keeped */
                        userDetails.getAuthorities());

                authBadge.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authBadge);
                System.out.println("🎉 JWT Filter: User authenticated successfully with authorities: "
                        + userDetails.getAuthorities());
            } else {
                System.out.println("❌ JWT Filter: Token is invalid or expired");
            }
        }

        /* Pass to the next filter */
        System.out.println("➡️ JWT Filter: Passing request to next filter in chain");
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request URI is for a public endpoint that doesn't require
     * authentication
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.equals("/api/register") ||
                requestURI.equals("/api/login") ||
                requestURI.startsWith("/actuator/");
    }
}