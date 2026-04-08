package com.openclassrooms.etudiant.exception;

import com.openclassrooms.etudiant.enums.ExceptionErrorMessage;
import com.openclassrooms.etudiant.handler.ErrorDetails;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNIT TESTS FOR GLOBAL EXCEPTION HANDLER
 * Tests each @ExceptionHandler method via direct invocation using
 * MockHttpServletRequest wrapped in ServletWebRequest.
 * Covers all exception-to-HTTP-status mappings and error codes.
 */
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        /* WRAP A MOCK REQUEST IN ServletWebRequest FOR WebRequest PARAMETER */
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/test");
        webRequest = new ServletWebRequest(mockRequest);
    }

    /** ENTITY NOT FOUND */
    @Nested
    @DisplayName("handleEntityNotFound - EntityNotFoundException → 404")
    class EntityNotFoundTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 404 with ENTITY_NOT_FOUND error code")
        void shouldReturn404_WithEntityNotFoundCode() {
            /* ARRANGE */
            EntityNotFoundException ex = new EntityNotFoundException("Student not found");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleEntityNotFound(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
            assertEquals("ENTITY_NOT_FOUND", response.getBody().getErrorCode());
            assertEquals("Student not found", response.getBody().getMessage());
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return fallback message when exception message is null")
        void shouldReturnFallbackMessage_WhenExceptionMessageIsNull() {
            /* ARRANGE - EntityNotFoundException with no message */
            EntityNotFoundException ex = new EntityNotFoundException((String) null);

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleEntityNotFound(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
            assertEquals(ExceptionErrorMessage.ENTITY_NOT_FOUND_FALLBACK.getMessage(), response.getBody().getMessage());
        }
    }

    /** DATABASE ERRORS */
    @Nested
    @DisplayName("handleDatabaseErrors - DataIntegrityViolationException")
    class DatabaseErrorTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 409 CONFLICT when a UNIQUE constraint is violated")
        void shouldReturn409_WhenUniqueConstraintViolated() {
            /* ARRANGE - cause message contains "UNIQUE" */
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement",
                    new RuntimeException("UNIQUE constraint failed: users.login"));

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleDatabaseErrors(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
            assertEquals("DUPLICATE_ENTRY", response.getBody().getErrorCode());
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 409 CONFLICT when a Duplicate key exception occurs")
        void shouldReturn409_WhenDuplicateKeyException() {
            /* ARRANGE - cause message contains "Duplicate" */
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement",
                    new RuntimeException("Duplicate entry 'test@test.com' for key 'login'"));

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleDatabaseErrors(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
            assertEquals("DUPLICATE_ENTRY", response.getBody().getErrorCode());
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 422 UNPROCESSABLE_ENTITY for other constraint violations")
        void shouldReturn422_WhenOtherConstraintViolated() {
            /* ARRANGE - cause message does NOT contain "Duplicate" or "UNIQUE" */
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement",
                    new RuntimeException("foreign key constraint failed"));

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleDatabaseErrors(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatusCode().value());
            assertEquals("DATA_INTEGRITY_ERROR", response.getBody().getErrorCode());
        }
    }

    /** AUTHENTICATION ERRORS */
    @Nested
    @DisplayName("handleAuthenticationErrors - Authentication exceptions → 401")
    class AuthenticationErrorTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 401 for BadCredentialsException")
        void shouldReturn401_ForBadCredentialsException() {
            /* ARRANGE */
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleAuthenticationErrors(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
            assertEquals("AUTHENTICATION_FAILED", response.getBody().getErrorCode());
            assertEquals(ExceptionErrorMessage.INVALID_CREDENTIALS.getMessage(), response.getBody().getMessage());
        }
    }

    /** ACCESS DENIED */
    @Nested
    @DisplayName("handleAccessDenied - AccessDeniedException → 403")
    class AccessDeniedTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 403 FORBIDDEN for AccessDeniedException")
        void shouldReturn403_ForAccessDeniedException() {
            /* ARRANGE */
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleAccessDenied(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
            assertEquals("ACCESS_DENIED", response.getBody().getErrorCode());
            assertEquals(ExceptionErrorMessage.ACCESS_DENIED.getMessage(),
                    response.getBody().getMessage());
        }
    }

    /** HTTP METHOD NOT SUPPORTED */
    @Nested
    @DisplayName("handleMethodNotSupported - HttpRequestMethodNotSupportedException → 405")
    class MethodNotSupportedTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 405 and include the unsupported method name in message")
        void shouldReturn405_WithMethodNameInMessage() {
            /* ARRANGE */
            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleMethodNotSupported(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getStatusCode().value());
            assertEquals("METHOD_NOT_SUPPORTED", response.getBody().getErrorCode());
            assertTrue(response.getBody().getMessage().contains("DELETE"));
        }
    }

    /** ILLEGAL ARGUMENT */
    @Nested
    @DisplayName("handleIllegalArgument - IllegalArgumentException → 400")
    class IllegalArgumentTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 400 with INVALID_ARGUMENT error code")
        void shouldReturn400_WithInvalidArgumentCode() {
            /* ARRANGE */
            IllegalArgumentException ex = new IllegalArgumentException("Invalid value provided");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleIllegalArgument(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
            assertEquals("INVALID_ARGUMENT", response.getBody().getErrorCode());
            assertEquals("Invalid value provided", response.getBody().getMessage());
        }

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return fallback message when IllegalArgumentException message is null")
        void shouldReturnFallbackMessage_WhenMessageIsNull() {
            /* ARRANGE */
            IllegalArgumentException ex = new IllegalArgumentException((String) null);

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleIllegalArgument(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
            assertEquals(ExceptionErrorMessage.INVALID_ARGUMENT_FALLBACK.getMessage(), response.getBody().getMessage());
        }
    }

    /** RUNTIME EXCEPTION */
    @Nested
    @DisplayName("handleRuntimeException - RuntimeException → 500")
    class RuntimeExceptionTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR for RuntimeException")
        void shouldReturn500_ForRuntimeException() {
            /* ARRANGE */
            RuntimeException ex = new RuntimeException("Unexpected processing error");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleRuntimeException(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
            assertEquals("RUNTIME_ERROR", response.getBody().getErrorCode());
        }
    }

    /** GENERIC EXCEPTION */
    @Nested
    @DisplayName("handleGenericException - Exception → 500")
    class GenericExceptionTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR for any unhandled Exception")
        void shouldReturn500_ForGenericException() throws Exception {
            /* ARRANGE */
            Exception ex = new Exception("Something went very wrong");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleGenericException(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
            assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
            assertEquals(ExceptionErrorMessage.INTERNAL_ERROR.getMessage(), response.getBody().getMessage());
        }
    }
}
