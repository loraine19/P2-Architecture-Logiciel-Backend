package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RefreshTokenDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.LoginResponse;
import com.openclassrooms.etudiant.dto.dtoHelpers.MessageResp;
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

    /* REGISTER */
    @PostMapping("/api/register")
    public ResponseEntity<MessageResp> register(@Valid @RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    /* LOGIN */
    @PostMapping("/api/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        return userService.login(loginRequestDTO, request, response);
    }

    /* LOGOUT */
    @PostMapping("/api/logout")
    public ResponseEntity<MessageResp> logout(HttpServletResponse response) {
        return userService.logout(response);
    }

    /* REFRESH TOKEN */
    @PostMapping("/api/refresh")
    public ResponseEntity<MessageResp> refresh(
            @Valid @RequestBody RefreshTokenDTO refreshTokenDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        return userService.refresh(refreshTokenDTO.getRefreshToken(), request, response);
    }

}
