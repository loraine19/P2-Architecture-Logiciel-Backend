package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (SecurityConstants.isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = getJwtFromRequest(request, response);
        if (jwt == null) {
            log.debug("No JWT token found in cookies or headers for: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        if (jwt.trim().isEmpty() || !jwt.contains(".")) {
            log.warn("Malformed JWT token received from: {}", getClientIP(request));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Malformed JWT token");
            return;
        }

        try {
            String userLogin = jwtService.extractUsername(jwt, false);
            if (userLogin != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userLogin);

                if (jwtService.isTokenValid(jwt, userDetails, false)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
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

        filterChain.doFilter(request, response);
    }

    /** PRIVATE METHODS */

    /* GET JWT FROM REQUEST */
    private String getJwtFromRequest(HttpServletRequest request, HttpServletResponse response) {
        String jwtFromCookies = jwtService.getJwtFromCookies(request, false);
        if (jwtFromCookies != null)
            return jwtFromCookies;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer "))
            return authHeader.substring(7);

        return null;
    }

    /* GET CLIENT IP */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

}