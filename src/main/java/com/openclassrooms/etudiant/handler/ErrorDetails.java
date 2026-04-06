package com.openclassrooms.etudiant.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response format
 * Contains detailed error information for API responses
 * Used by global exception handler for consistent error structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;

    private String message;
    private String path;

    // null when no field-level validation errors
    private Map<String, String> validationErrors;
}
