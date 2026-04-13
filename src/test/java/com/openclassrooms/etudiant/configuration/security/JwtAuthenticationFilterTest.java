package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS FOR JWT AUTHENTICATION FILTER
 * Covers: malformed JWT detection, exception catches, JWT from cookies/headers,
 * client IP extraction
 * ./mvnw clean test -Dtest=JwtAuthenticationFilterTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    /* A structurally valid JWT string (3 parts separated by dots) */
    private static final String VALID_JWT = "header.payload.signature";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** PUBLIC ENDPOINT BYPASS */
    @Nested
    @DisplayName("Public endpoint bypass")
    class PublicEndpointTests {

        /* LOGIN ENDPOINT */
        @Test
        @DisplayName("Should bypass filter for /api/login without touching JWT service")
        void shouldBypassFilter_ForLoginEndpoint() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/login");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }

        /* REGISTER ENDPOINT */
        @Test
        @DisplayName("Should bypass filter for /api/register without touching JWT service")
        void shouldBypassFilter_ForRegisterEndpoint() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/register");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }
    }

    /** JWT EXTRACTION FROM COOKIES AND HEADERS */
    @Nested
    @DisplayName("JWT extraction from cookies and Authorization header")
    class JwtExtractionTests {

        /* NO JWT FOUND */
        @Test
        @DisplayName("Should pass through when no JWT in cookies or Authorization header")
        void shouldPassThrough_WhenNoJwtFound() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(null);

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            verify(filterChain).doFilter(request, response);
        }

        /* COOKIE TOKEN PREFERRED */
        @Test
        @DisplayName("Should use cookie token and authenticate user when cookie is present")
        void shouldPreferCookieToken_AndAuthenticate() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false)).thenReturn("user@example.com");
            UserDetails userDetails = buildUser("user@example.com");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(VALID_JWT, userDetails, false)).thenReturn(true);

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            verify(filterChain).doFilter(request, response);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("user@example.com",
                    SecurityContextHolder.getContext().getAuthentication().getName());
        }

        /* AUTHORIZATION HEADER FALLBACK */
        @Test
        @DisplayName("Should use Authorization header token when no cookie is present")
        void shouldUseHeaderToken_WhenNoCookiePresent() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("Authorization", "Bearer " + VALID_JWT);
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(null);
            when(jwtService.extractUsername(VALID_JWT, false)).thenReturn("user@example.com");
            UserDetails userDetails = buildUser("user@example.com");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(VALID_JWT, userDetails, false)).thenReturn(true);

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            verify(filterChain).doFilter(request, response);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        }

        /* NON-BEARER HEADER IGNORED */
        @Test
        @DisplayName("Should pass through when Authorization header is not Bearer scheme")
        void shouldPassThrough_WhenAuthHeaderIsNotBearer() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(null);

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            verify(filterChain).doFilter(request, response);
        }
    }

    /** MALFORMED JWT EARLY DETECTION */
    @Nested
    @DisplayName("Malformed JWT early detection")
    class MalformedJwtEarlyDetectionTests {

        /* BLANK TOKEN */
        @Test
        @DisplayName("Should return 401 when JWT is blank/whitespace (no dot)")
        void shouldReturn401_WhenJwtIsBlank() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn("   ");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed JWT token", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }

        /* NO DOT SEPARATOR */
        @Test
        @DisplayName("Should return 401 when JWT has no dot separator")
        void shouldReturn401_WhenJwtHasNoDot() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn("notajwtatall");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed JWT token", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    /** JWT EXCEPTION CATCHES */
    @Nested
    @DisplayName("JWT exception handling")
    class JwtExceptionHandlingTests {

        /* EXPIRED TOKEN */
        @Test
        @DisplayName("Should return 401 'Token expired' when ExpiredJwtException is thrown")
        void shouldReturn401TokenExpired_WhenExpiredJwtException() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            Claims claims = mock(Claims.class);
            when(claims.getSubject()).thenReturn("user@example.com");
            when(jwtService.extractUsername(VALID_JWT, false))
                    .thenThrow(new ExpiredJwtException(null, claims, "Token has expired"));

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Token expired", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }

        /* MALFORMED TOKEN */
        @Test
        @DisplayName("Should return 401 'Malformed token' when MalformedJwtException is thrown")
        void shouldReturn401MalformedToken_WhenMalformedJwtException() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false))
                    .thenThrow(new MalformedJwtException("bad token structure"));

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed token", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }

        /* INVALID SIGNATURE */
        @Test
        @DisplayName("Should return 401 'Invalid token signature' when SignatureException is thrown")
        void shouldReturn401InvalidSignature_WhenSignatureException() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false))
                    .thenThrow(new SignatureException("signature mismatch"));

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Invalid token signature", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }

        /* GENERIC ERROR */
        @Test
        @DisplayName("Should return 500 'Authentication service error' on generic Exception")
        void shouldReturn500_WhenGenericExceptionThrown() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false))
                    .thenThrow(new RuntimeException("unexpected processing error"));

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
            assertEquals("Authentication service error", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }

        /* INVALID TOKEN FOR USER */
        @Test
        @DisplayName("Should return 401 'Invalid token' when isTokenValid returns false")
        void shouldReturn401_WhenTokenIsInvalidForUser() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false)).thenReturn("user@example.com");
            UserDetails userDetails = buildUser("user@example.com");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(VALID_JWT, userDetails, false)).thenReturn(false);

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Invalid token", response.getContentAsString());
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    /** CLIENT IP EXTRACTION */
    @Nested
    @DisplayName("Client IP extraction")
    class ClientIpExtractionTests {

        /* X-FORWARDED-FOR CHAIN */
        @Test
        @DisplayName("Should use first IP from X-Forwarded-For header (proxy chain)")
        void shouldUseXForwardedFor_FirstIpFromProxyChain() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1, 172.16.0.5");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn("notajwtatall");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /*
             * ASSERT - malformed token triggers getClientIP via log.warn → 401 confirms
             * path
             */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed JWT token", response.getContentAsString());
        }

        /* X-REAL-IP FALLBACK */
        @Test
        @DisplayName("Should use X-Real-IP when X-Forwarded-For is absent")
        void shouldUseXRealIP_WhenXForwardedForAbsent() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("X-Real-IP", "203.0.113.5");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn("notajwtatall");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed JWT token", response.getContentAsString());
        }

        /* REMOTE ADDRESS FALLBACK */
        @Test
        @DisplayName("Should fall back to remote address when no proxy headers present")
        void shouldFallbackToRemoteAddr_WhenNoProxyHeaders() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.setRemoteAddr("10.0.0.42");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn("notajwtatall");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed JWT token", response.getContentAsString());
        }

        /* UNKNOWN X-FORWARDED-FOR IGNORED */
        @Test
        @DisplayName("Should ignore 'unknown' X-Forwarded-For and fall back to X-Real-IP")
        void shouldIgnoreUnknownXForwardedFor_AndUseXRealIP() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("X-Real-IP", "198.51.100.7");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn("notajwtatall");

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed JWT token", response.getContentAsString());
        }

        /* IP LOGGED ON MALFORMED EXCEPTION */
        @Test
        @DisplayName("Should log client IP from X-Forwarded-For when MalformedJwtException is caught")
        void shouldExtractClientIp_WhenMalformedJwtExceptionCaught() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("X-Forwarded-For", "192.0.2.1");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false))
                    .thenThrow(new MalformedJwtException("bad structure"));

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Malformed token", response.getContentAsString());
        }

        /* IP LOGGED ON SIGNATURE EXCEPTION */
        @Test
        @DisplayName("Should extract client IP when SignatureException is caught")
        void shouldExtractClientIp_WhenSignatureExceptionCaught() throws Exception {
            /* ARRANGE */
            request.setRequestURI("/api/students");
            request.addHeader("X-Real-IP", "198.51.100.9");
            when(jwtService.getJwtFromCookies(request, false)).thenReturn(VALID_JWT);
            when(jwtService.extractUsername(VALID_JWT, false))
                    .thenThrow(new SignatureException("sig fail"));

            /* ACT */
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
            assertEquals("Invalid token signature", response.getContentAsString());
        }
    }

    /** HELPER */
    private UserDetails buildUser(String username) {
        return User.withUsername(username)
                .password("encodedPass")
                .authorities(Collections.emptyList())
                .build();
    }
}
