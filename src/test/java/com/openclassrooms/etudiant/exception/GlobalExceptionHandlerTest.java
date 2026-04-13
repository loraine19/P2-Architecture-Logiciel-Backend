package com.openclassrooms.etudiant.exception;

import com.openclassrooms.etudiant.handler.ErrorDetails;
import com.openclassrooms.etudiant.messages.ExceptionErrorMessage;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

        // : branche AuthenticationException générique dans
        // handleAuthenticationErrors() non couverte
        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 401 for a generic AuthenticationException (not only BadCredentials)")
        void shouldReturn401_ForGenericAuthenticationException() {
            /*
             * ARRANGE - InsufficientAuthenticationException extends AuthenticationException
             */
            InsufficientAuthenticationException ex = new InsufficientAuthenticationException("Token missing");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleAuthenticationErrors(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
            assertEquals("AUTHENTICATION_FAILED", response.getBody().getErrorCode());
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

    // : handleValidationErrors() non testé directement
    /** VALIDATION ERRORS */
    @Nested
    @DisplayName("handleValidationErrors - MethodArgumentNotValidException → 400")
    class ValidationErrorTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 400 with VALIDATION_ERROR code and field errors map")
        void shouldReturn400_WithValidationErrorCode() throws Exception {
            /* ARRANGE - build a MethodArgumentNotValidException with one FieldError */
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userDTO");
            bindingResult.addError(new FieldError("userDTO", "login", "must not be blank"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleValidationErrors(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
            assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
            assertNotNull(response.getBody().getValidationErrors());
            assertTrue(response.getBody().getValidationErrors().containsKey("login"));
        }
    }

    // : handleResourceNotFound() non testé directement
    /** RESOURCE NOT FOUND */
    @Nested
    @DisplayName("handleResourceNotFound - NoResourceFoundException → 404")
    class ResourceNotFoundTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 404 with RESOURCE_NOT_FOUND error code")
        void shouldReturn404_ForNoResourceFoundException() {
            /* ARRANGE */
            NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/unknown");

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleResourceNotFound(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
            assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
            assertTrue(response.getBody().getMessage().contains("/api/unknown"));
        }
    }

    // : handleTypeMismatch() non testé directement
    /** TYPE MISMATCH */
    @Nested
    @DisplayName("handleTypeMismatch - MethodArgumentTypeMismatchException → 400")
    class TypeMismatchTests {

        @SuppressWarnings("null")
        @Test
        @DisplayName("Should return 400 with TYPE_MISMATCH error code")
        void shouldReturn400_WithTypeMismatchCode() throws Exception {
            /* ARRANGE - pass null MethodParameter (not used by handler) */
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "id", null, null);

            /* ACT */
            ResponseEntity<ErrorDetails> response = handler.handleTypeMismatch(ex, webRequest);

            /* ASSERT */
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
            assertEquals("TYPE_MISMATCH", response.getBody().getErrorCode());
            assertTrue(response.getBody().getMessage().contains("abc"));
            assertTrue(response.getBody().getMessage().contains("id"));
        }
    }

    /** EXCEPTION ERROR MESSAGE ENUM */
    @Nested
    @DisplayName("ExceptionErrorMessage - Enum messages and format")
    class ExceptionErrorMessageTests {

        /* GET MESSAGE */
        @Test
        @DisplayName("Should return non-blank message for every enum value")
        void shouldReturnNonBlankMessage_ForEveryValue() {
            /* ACT & ASSERT */
            for (ExceptionErrorMessage msg : ExceptionErrorMessage.values()) {
                assertNotNull(msg.getMessage());
                assertFalse(msg.getMessage().isBlank());
            }
        }

        /* FORMAT WITH PLACEHOLDER */
        @Test
        @DisplayName("Should format RESOURCE_NOT_FOUND with the resource path")
        void shouldFormatResourceNotFound_WithPath() {
            /* ACT */
            String result = ExceptionErrorMessage.RESOURCE_NOT_FOUND.format("/api/unknown");

            /* ASSERT */
            assertTrue(result.contains("/api/unknown"));
        }

        /* FORMAT METHOD NOT SUPPORTED */
        @Test
        @DisplayName("Should format METHOD_NOT_SUPPORTED with the HTTP method")
        void shouldFormatMethodNotSupported_WithMethod() {
            /* ACT */
            String result = ExceptionErrorMessage.METHOD_NOT_SUPPORTED.format("DELETE");

            /* ASSERT */
            assertTrue(result.contains("DELETE"));
        }

        /* FORMAT TYPE MISMATCH */
        @Test
        @DisplayName("Should format TYPE_MISMATCH with value and parameter name")
        void shouldFormatTypeMismatch_WithValueAndName() {
            /* ACT */
            String result = ExceptionErrorMessage.TYPE_MISMATCH.format("abc", "id");

            /* ASSERT */
            assertTrue(result.contains("abc"));
            assertTrue(result.contains("id"));
        }
    }

    /** ERROR DETAILS BUILDER */
    @Nested
    @DisplayName("ErrorDetails - Builder, getters and equals")
    class ErrorDetailsTests {

        /* BUILDER ALL FIELDS */
        @Test
        @DisplayName("Should build ErrorDetails with all fields set")
        void shouldBuildErrorDetails_WithAllFields() {
            /* ARRANGE */
            LocalDateTime now = LocalDateTime.now();

            /* ACT */
            ErrorDetails details = ErrorDetails.builder()
                    .timestamp(now)
                    .status(404)
                    .error("Not Found")
                    .errorCode("ENTITY_NOT_FOUND")
                    .message("Student not found")
                    .path("/api/students/99")
                    .build();

            /* ASSERT */
            assertEquals(now, details.getTimestamp());
            assertEquals(404, details.getStatus());
            assertEquals("Not Found", details.getError());
            assertEquals("ENTITY_NOT_FOUND", details.getErrorCode());
            assertEquals("Student not found", details.getMessage());
            assertEquals("/api/students/99", details.getPath());
            assertNull(details.getValidationErrors());
        }

        /* BUILDER WITH VALIDATION ERRORS */
        @Test
        @DisplayName("Should build ErrorDetails with validationErrors map")
        void shouldBuildErrorDetails_WithValidationErrors() {
            /* ARRANGE */
            Map<String, String> errors = Map.of("login", "must not be blank");

            /* ACT */
            ErrorDetails details = ErrorDetails.builder()
                    .status(400)
                    .error("Bad Request")
                    .errorCode("VALIDATION_ERROR")
                    .message("Validation failed")
                    .validationErrors(errors)
                    .build();

            /* ASSERT */
            assertNotNull(details.getValidationErrors());
            assertEquals("must not be blank", details.getValidationErrors().get("login"));
        }

        /* NO-ARGS CONSTRUCTOR */
        @Test
        @DisplayName("Should create empty ErrorDetails via no-args constructor")
        void shouldCreateEmptyErrorDetails_ViaNoArgsConstructor() {
            /* ACT */
            ErrorDetails details = new ErrorDetails();

            /* ASSERT */
            assertNull(details.getMessage());
            assertNull(details.getErrorCode());
            assertEquals(0, details.getStatus());
        }

        /* SETTERS */
        @Test
        @DisplayName("Should update fields via setters generated by @Data")
        void shouldUpdateFields_ViaSetters() {
            /* ARRANGE */
            ErrorDetails details = new ErrorDetails();

            /* ACT */
            details.setStatus(500);
            details.setMessage("Error occurred");
            details.setPath("/api/test");

            /* ASSERT */
            assertEquals(500, details.getStatus());
            assertEquals("Error occurred", details.getMessage());
            assertEquals("/api/test", details.getPath());
        }

        /* EQUALITY */
        @Test
        @DisplayName("Should be equal to another ErrorDetails built with identical values")
        void shouldBeEqual_WhenBuiltWithSameValues() {
            /* ARRANGE */
            LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 12, 0);
            ErrorDetails d1 = ErrorDetails.builder().timestamp(ts).status(404)
                    .errorCode("NOT_FOUND").message("Not found").build();
            ErrorDetails d2 = ErrorDetails.builder().timestamp(ts).status(404)
                    .errorCode("NOT_FOUND").message("Not found").build();

            /* ASSERT */
            assertEquals(d1, d2);
            assertEquals(d1.hashCode(), d2.hashCode());
        }
    }
}
