package com.openclassrooms.etudiant.service.Interfaces;

import org.springframework.http.ResponseEntity;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.entities.MessageResp;

import jakarta.servlet.http.HttpServletResponse;

public interface UserServiceInterface {
    MessageResp register(UserDTO userDTO);

    MessageResp login(LoginRequestDTO loginRequestDTO, HttpServletResponse response);

    void setAuthCookie(HttpServletResponse response, String token);

    ResponseEntity<Void> logout(HttpServletResponse response);
}