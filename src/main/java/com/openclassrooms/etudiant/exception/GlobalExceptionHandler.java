package com.openclassrooms.etudiant.exception;

import com.openclassrooms.etudiant.handler.ErrorDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application
 * Centralizes error handling with consistent response format
 * Provides logging and proper HTTP status codes
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** PUBLIC METHODS */

    /* VALIDATION ERRORS */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed for one or more fields")
                .path(extractPath(request))
                .validationErrors(fieldErrors)
                .build();

        log.warn("Validation error on {}: {}", extractPath(request), fieldErrors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    /* DATABASE ERRORS */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDatabaseErrors(DataIntegrityViolationException ex, WebRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String rootCause = (cause != null && cause.getMessage() != null) ? cause.getMessage() : "";

        if (rootCause.contains("Duplicate") || rootCause.contains("UNIQUE")) {
            return createErrorResponse(
                    "DUPLICATE_ENTRY",
                    "Resource already exists - duplicate entry detected",
                    HttpStatus.CONFLICT, request);
        }

        return createErrorResponse(
                "DATA_INTEGRITY_ERROR",
                "Database constraint violation",
                HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    /* ENTITY NOT FOUND */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        return createErrorResponse(
                "ENTITY_NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Requested entity not found",
                HttpStatus.NOT_FOUND, request);
    }

    /* AUTHENTICATION ERRORS */
    @ExceptionHandler({ BadCredentialsException.class, AuthenticationException.class })
    public ResponseEntity<ErrorDetails> handleAuthenticationErrors(Exception ex, WebRequest request) {
        return createErrorResponse(
                "AUTHENTICATION_FAILED",
                "Invalid credentials provided",
                HttpStatus.UNAUTHORIZED, request);
    }

    /* ACCESS DENIED */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return createErrorResponse(
                "ACCESS_DENIED",
                "Insufficient permissions to access this resource",
                HttpStatus.FORBIDDEN, request);
    }

    /* HTTP METHOD NOT SUPPORTED */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDetails> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            WebRequest request) {
        return createErrorResponse(
                "METHOD_NOT_SUPPORTED",
                "HTTP method '" + ex.getMethod() + "' not supported for this endpoint",
                HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    /* RESOURCE NOT FOUND */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFound(NoResourceFoundException ex, WebRequest request) {
        return createErrorResponse(
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found: " + ex.getResourcePath(),
                HttpStatus.NOT_FOUND, request);
    }

    /* TYPE MISMATCH - e.g. non-numeric path variable where Long is expected */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetails> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        return createErrorResponse(
                "TYPE_MISMATCH",
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'",
                HttpStatus.BAD_REQUEST, request);
    }

    /* ILLEGAL ARGUMENT */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return createErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided",
                HttpStatus.BAD_REQUEST, request);
    }

    /* RUNTIME EXCEPTIONS */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDetails> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return createErrorResponse(
                "RUNTIME_ERROR",
                "An unexpected error occurred during request processing",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /* CATCH-ALL EXCEPTION HANDLER */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred", ex);
        return createErrorResponse(
                "INTERNAL_ERROR",
                "An internal server error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /* PRIVATE HELPER METHODS */

    /* Create standardized error response */
    private ResponseEntity<ErrorDetails> createErrorResponse(String errorCode, String message,
            HttpStatus status, WebRequest request) {

        if (status.is4xxClientError()) {
            log.warn("[{}] {}: {}", status.value(), errorCode, message);
        } else {
            log.error("[{}] {}: {}", status.value(), errorCode, message);
        }

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(extractPath(request))
                .build();

        return new ResponseEntity<>(errorDetails, status);
    }

    /* Extract path from WebRequest for error details */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }

}