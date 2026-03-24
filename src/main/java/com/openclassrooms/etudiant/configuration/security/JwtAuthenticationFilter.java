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

        System.out.println("🔐 JWT Filter: Processing request for " + request.getRequestURI());

        /* Check if header is missing or wrong format */
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ JWT Filter: No valid Authorization header found - skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        /* Extract token and username */
        System.out.println("📝 JWT Filter: Found Bearer token, extracting...");
        final String jwt = authHeader.substring(7);
        final String userLogin = jwtService.extractUsername(jwt);
        System.out.println("👤 JWT Filter: Extracted username: " + userLogin);

        /* Check if user is not already authenticated */
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
}