package com.openclassrooms.etudiant.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Create standardized error response
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorCode, String message,
            HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, status);
    }

    // Validation errors (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("errorCode", "VALIDATION_ERROR");
        response.put("message", "Les données fournies ne sont pas valides");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Database integrity errors
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseErrors(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();

        if (message.contains("Duplicate") || message.contains("UNIQUE")) {
            return createErrorResponse(
                    "DUPLICATE_ENTRY",
                    "Cette donnée existe déjà (conflit de duplication)",
                    HttpStatus.CONFLICT);
        }

        return createErrorResponse(
                "DATA_INTEGRITY_ERROR",
                "Erreur d'intégrité des données : " + message,
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Authentication errors
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return createErrorResponse(
                "INVALID_CREDENTIALS",
                "Identifiants invalides",
                HttpStatus.UNAUTHORIZED);
    }

    // Access denied errors
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return createErrorResponse(
                "ACCESS_DENIED",
                "Accès refusé - permissions insuffisantes",
                HttpStatus.FORBIDDEN);
    }

    // HTTP method not supported
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return createErrorResponse(
                "METHOD_NOT_SUPPORTED",
                "Méthode HTTP non supportée : " + ex.getMethod(),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Resource not found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(NoResourceFoundException ex) {
        return createErrorResponse(
                "RESOURCE_NOT_FOUND",
                "Ressource non trouvée : " + ex.getResourcePath(),
                HttpStatus.NOT_FOUND);
    }

    // Illegal arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return createErrorResponse(
                "INVALID_ARGUMENT",
                "Argument invalide : " + ex.getMessage(),
                HttpStatus.BAD_REQUEST);
    }

    // Runtime exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return createErrorResponse(
                "RUNTIME_ERROR",
                "Erreur d'exécution : " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Catch all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        return createErrorResponse(
                "INTERNAL_ERROR",
                "Une erreur interne s'est produite : " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}