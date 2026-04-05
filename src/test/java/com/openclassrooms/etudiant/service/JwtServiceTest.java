package com.openclassrooms.etudiant.service;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNIT TESTS FOR JWT SERVICE
 * Tests token generation, validation, extraction from cookies and headers
 * ./mvnw clean test -Dtest=JwtServiceTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUserDetails;
    private UserDetails otherUserDetails;

    /* 32-byte secrets encoded in Base64 - minimum for HMAC-SHA256 */
    private static final String ACCESS_SECRET = Base64.getEncoder()
            .encodeToString("testSecretKeyForJwtUnitTestingOK".getBytes(StandardCharsets.UTF_8));
    private static final String REFRESH_SECRET = Base64.getEncoder()
            .encodeToString("refreshSecretKeyForJwtUnitTestOK".getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        /* INITIALIZE SERVICE WITH INJECTED @Value FIELDS */
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", ACCESS_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshSecret", REFRESH_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpirationMs", 172800000L);

        /* INITIALIZE TEST USER DETAILS */
        testUserDetails = User.withUsername("jean.dupont@example.com")
                .password("encodedPassword")
                .authorities(Collections.emptyList())
                .build();

        otherUserDetails = User.withUsername("other.user@example.com")
                .password("encodedPassword")
                .authorities(Collections.emptyList())
                .build();
    }

    /** GENERATE TOKEN */
    @Nested
    @DisplayName("generateToken - Token Generation")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate a valid access token (3 JWT parts)")
        void shouldGenerateAccessToken_WhenCalledWithAccessFlag() {
            /* ACT */
            String token = jwtService.generateToken(testUserDetails, false);

            /* ASSERT - JWT format: header.payload.signature */
            assertNotNull(token);
            assertFalse(token.isBlank());
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("Should generate a valid refresh token (3 JWT parts)")
        void shouldGenerateRefreshToken_WhenCalledWithRefreshFlag() {
            /* ACT */
            String token = jwtService.generateToken(testUserDetails, true);

            /* ASSERT */
            assertNotNull(token);
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("Should generate different tokens for access and refresh (different secrets)")
        void shouldGenerateDifferentTokens_ForAccessAndRefresh() {
            /* ACT */
            String accessToken = jwtService.generateToken(testUserDetails, false);
            String refreshToken = jwtService.generateToken(testUserDetails, true);

            /* ASSERT - different signing keys produce different tokens */
            assertNotEquals(accessToken, refreshToken);
        }
    }

    /** EXTRACT USERNAME */
    @Nested
    @DisplayName("extractUsername - Username Extraction")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract correct username from access token")
        void shouldExtractUsername_FromAccessToken() {
            /* ARRANGE */
            String token = jwtService.generateToken(testUserDetails, false);

            /* ACT */
            String username = jwtService.extractUsername(token, false);

            /* ASSERT */
            assertEquals("jean.dupont@example.com", username);
        }

        @Test
        @DisplayName("Should extract correct username from refresh token")
        void shouldExtractUsername_FromRefreshToken() {
            /* ARRANGE */
            String token = jwtService.generateToken(testUserDetails, true);

            /* ACT */
            String username = jwtService.extractUsername(token, true);

            /* ASSERT */
            assertEquals("jean.dupont@example.com", username);
        }
    }

    /** IS TOKEN VALID */
    @Nested
    @DisplayName("isTokenValid - Token Validation")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should return true when access token is valid and user matches")
        void shouldReturnTrue_WhenTokenValidAndUserMatches() {
            /* ARRANGE */
            String token = jwtService.generateToken(testUserDetails, false);

            /* ACT */
            boolean result = jwtService.isTokenValid(token, testUserDetails, false);

            /* ASSERT */
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when refresh token is valid and user matches")
        void shouldReturnTrue_WhenRefreshTokenValidAndUserMatches() {
            /* ARRANGE */
            String token = jwtService.generateToken(testUserDetails, true);

            /* ACT */
            boolean result = jwtService.isTokenValid(token, testUserDetails, true);

            /* ASSERT */
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when userDetails is null")
        void shouldReturnFalse_WhenUserDetailsIsNull() {
            /* ARRANGE */
            String token = jwtService.generateToken(testUserDetails, false);

            /* ACT */
            boolean result = jwtService.isTokenValid(token, null, false);

            /* ASSERT */
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when token subject does not match userDetails username")
        void shouldReturnFalse_WhenUsernameDoesNotMatch() {
            /* ARRANGE - token for testUser, validate against otherUser */
            String token = jwtService.generateToken(testUserDetails, false);

            /* ACT */
            boolean result = jwtService.isTokenValid(token, otherUserDetails, false);

            /* ASSERT */
            assertFalse(result);
        }

        @Test
        @DisplayName("Should throw ExpiredJwtException when token is expired")
        void shouldThrowExpiredJwtException_WhenTokenIsExpired() {
            /* ARRANGE - generate token that expired 1 minute ago */
            ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -60000L);
            String expiredToken = jwtService.generateToken(testUserDetails, false);
            ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);

            /* ACT & ASSERT */
            assertThrows(ExpiredJwtException.class,
                    () -> jwtService.isTokenValid(expiredToken, testUserDetails, false));
        }
    }

    /** GET JWT FROM COOKIES */
    @Nested
    @DisplayName("getJwtFromCookies - Cookie Extraction")
    class GetJwtFromCookiesTests {

        @Test
        @DisplayName("Should return access token from 'token' cookie")
        void shouldReturnAccessToken_WhenTokenCookieIsPresent() {
            /* ARRANGE */
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("token", "mock.access.token"));

            /* ACT */
            String result = jwtService.getJwtFromCookies(request, false);

            /* ASSERT */
            assertEquals("mock.access.token", result);
        }

        @Test
        @DisplayName("Should return refresh token from 'refreshToken' cookie")
        void shouldReturnRefreshToken_WhenRefreshTokenCookieIsPresent() {
            /* ARRANGE */
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("refreshToken", "mock.refresh.token"));

            /* ACT */
            String result = jwtService.getJwtFromCookies(request, true);

            /* ASSERT */
            assertEquals("mock.refresh.token", result);
        }

        @Test
        @DisplayName("Should return null when no cookie is present")
        void shouldReturnNull_WhenNoCookiePresent() {
            /* ARRANGE */
            MockHttpServletRequest request = new MockHttpServletRequest();

            /* ACT */
            String result = jwtService.getJwtFromCookies(request, false);

            /* ASSERT */
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null when cookie value is blank")
        void shouldReturnNull_WhenCookieValueIsBlank() {
            /* ARRANGE */
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("token", "   "));

            /* ACT */
            String result = jwtService.getJwtFromCookies(request, false);

            /* ASSERT */
            assertNull(result);
        }
    }

    /** GET JWT FROM HEADER */
    @Nested
    @DisplayName("getJwtFromHeader - Header Extraction")
    class GetJwtFromHeaderTests {

        @Test
        @DisplayName("Should return token when valid Bearer Authorization header is present")
        void shouldReturnToken_WhenBearerHeaderIsPresent() {
            /* ARRANGE */
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer mock.jwt.token");

            /* ACT */
            String result = jwtService.getJwtFromHeader(request);

            /* ASSERT */
            assertEquals("mock.jwt.token", result);
        }

        @Test
        @DisplayName("Should return null when Authorization header is absent")
        void shouldReturnNull_WhenAuthorizationHeaderIsAbsent() {
            /* ARRANGE */
            MockHttpServletRequest request = new MockHttpServletRequest();

            /* ACT */
            String result = jwtService.getJwtFromHeader(request);

            /* ASSERT */
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null when Authorization header does not start with 'Bearer '")
        void shouldReturnNull_WhenHeaderIsNotBearer() {
            /* ARRANGE - Basic auth header */
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            /* ACT */
            String result = jwtService.getJwtFromHeader(request);

            /* ASSERT */
            assertNull(result);
        }
    }
}
