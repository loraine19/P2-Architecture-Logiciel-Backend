package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.service.interfaces.JwtServiceImp;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Slf4j
public class JwtService implements JwtServiceImp {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${JWT_REFRESH_SECRET}")
    private String jwtRefreshSecret;

    @Value("${JWT_EXPIRATION_MS:3600000}")
    private long jwtExpirationMs;

    @Value("${JWT_REFRESH_EXPIRATION_MS:172800000}")
    private long jwtRefreshExpirationMs;

    /** PUBLIC METHODS */

    /* GENERATE TOKEN */
    @Override
    public String generateToken(UserDetails userDetails, Boolean isRefresh) {
        long expiration = isRefresh ? jwtRefreshExpirationMs : jwtExpirationMs;
        SecretKey key = getSigningKey(isRefresh ? jwtRefreshSecret : jwtSecret);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /* EXTRACT ALL CLAIMS */
    private Claims extractAllClaims(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /* EXTRACT USERNAME */
    @Override
    public String extractUsername(String token, Boolean isRefresh) {
        SecretKey key = getSigningKey(isRefresh ? jwtRefreshSecret : jwtSecret);
        return extractAllClaims(token, key).getSubject();
    }

    /* IS TOKEN VALID */
    @Override
    public boolean isTokenValid(String token, UserDetails userDetails, Boolean isRefresh) {
        if (userDetails == null)
            return false;
        SecretKey key = getSigningKey(isRefresh ? jwtRefreshSecret : jwtSecret);
        Claims claims = extractAllClaims(token, key);
        if (claims == null)
            return false;
        boolean isUsernameCorrect = claims.getSubject().equals(userDetails.getUsername());
        boolean isNotExpired = claims.getExpiration().after(new Date());
        return isUsernameCorrect && isNotExpired;
    }

    /* GET JWT FROM COOKIES */
    @Override
    public String getJwtFromCookies(HttpServletRequest request, Boolean isRefresh) {
        return extractCookieValue(request, isRefresh ? "refreshToken" : "token");
    }

    /* GET JWT FROM HEADER */
    @Override
    public String getJwtFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer "))
            return authHeader.substring(7);
        return null;
    }

    /** PRIVATE METHODS */

    /** GET SIGNING KEY */
    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /** EXTRACT COOKIE VALUE */
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return (cookie != null && !cookie.getValue().isBlank()) ? cookie.getValue() : null;
    }
}