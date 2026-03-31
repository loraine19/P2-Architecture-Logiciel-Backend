package com.openclassrooms.etudiant.service.interfaces;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.LoginResponse;
import com.openclassrooms.etudiant.dto.dtoHelpers.MessageResp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service interface for user authentication and management
 * Defines business operations for user registration and authentication
 */
public interface UserServiceInterface {

    MessageResp register(UserDTO userDTO);

    LoginResponse login(LoginRequestDTO loginRequestDTO, HttpServletRequest request,
            HttpServletResponse response);

    MessageResp logout(HttpServletResponse response);

    MessageResp refresh(String refreshToken, HttpServletRequest request, HttpServletResponse response);

}