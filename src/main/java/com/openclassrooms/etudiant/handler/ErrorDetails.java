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
    
    // Error timing and identification
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;
    
    // Error description and location
    private String message;
    private String path;
    
    // Optional detailed field errors (for validation failures)
    private Map<String, String> validationErrors;
}
