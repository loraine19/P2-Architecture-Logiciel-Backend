package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.LoginResponse;
import com.openclassrooms.etudiant.dto.MessageResp;
import com.openclassrooms.etudiant.dto.RefreshTokenDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication
 * Handles register, login and logout operations
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/api/register")
    public MessageResp register(@Valid @RequestBody UserDTO userDTO) {
        log.debug("Registration attempt for user: {}", userDTO.getLogin());
        MessageResp response = userService.register(userDTO);
        log.debug("Registration result for {}: {}", userDTO.getLogin(), response.getMessage());
        return response;
    }

    @PostMapping("/api/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginResponse auth = userService.login(loginRequestDTO, request, response);
        System.out.println("Login response: " + auth);
        return auth;
    }

    @PostMapping("/api/logout")
    public ResponseEntity<MessageResp> logout(HttpServletResponse response) {
        ResponseEntity<MessageResp> result = userService.logout(response);
        return result;
    }

    @PostMapping("/api/refresh")
    public ResponseEntity<MessageResp> refresh(
            @Valid @RequestBody RefreshTokenDTO refreshTokenDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        System.out.println("Received refresh token DTO: " + refreshTokenDTO);
        return userService.refresh(refreshTokenDTO.getRefreshToken(), request, response);
    }

}
