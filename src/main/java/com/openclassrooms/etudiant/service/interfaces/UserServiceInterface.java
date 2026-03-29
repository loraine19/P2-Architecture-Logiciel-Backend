package com.openclassrooms.etudiant.service.interfaces;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.LoginResponse;
import com.openclassrooms.etudiant.dto.MessageResp;
import com.openclassrooms.etudiant.dto.UserDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for user authentication and management
 * Defines business operations for user registration and authentication
 */
public interface UserServiceInterface {

    MessageResp register(UserDTO userDTO);

    LoginResponse login(LoginRequestDTO loginRequestDTO, HttpServletRequest request, HttpServletResponse response);

    ResponseEntity<MessageResp> logout(HttpServletResponse response);

    ResponseEntity<MessageResp> refresh(String refreshToken, HttpServletRequest request, HttpServletResponse response);

}