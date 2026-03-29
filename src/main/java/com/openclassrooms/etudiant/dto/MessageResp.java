package com.openclassrooms.etudiant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message response entity for API responses
 * Contains status messages and response info
 * Used across controllers for consistent messaging
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResp {

    // Response message - informative text for client
    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    /**
     * Create a success message response
     */
    public static MessageResp success(String message) {
        return MessageResp.builder()
                .message(message)
                .build();
    }

    /**
     * Create an error message response
     */
    public static MessageResp error(String message) {
        return MessageResp.builder()
                .message(message)
                .build();
    }
}
