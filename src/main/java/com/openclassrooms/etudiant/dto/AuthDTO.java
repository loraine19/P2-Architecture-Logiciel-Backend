package com.openclassrooms.etudiant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthDTO {
    @NotBlank
    private Boolean isAuthenticated;
    @NotBlank
    private String token;

    public String getPartialToken() {
        return token.length() > 5 ? token.substring(0, 5) : token;
    }

}
