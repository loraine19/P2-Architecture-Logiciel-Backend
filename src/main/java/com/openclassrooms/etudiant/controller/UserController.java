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
        MessageResp messageResp = userService.register(userDTO);
        return ResponseEntity.ok(messageResp);
    }

    /* LOGIN */
    @PostMapping("/api/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(loginRequestDTO, request, response);
        return ResponseEntity.ok(loginResponse);
    }

    /* LOGOUT */
    @PostMapping("/api/logout")
    public ResponseEntity<MessageResp> logout(HttpServletResponse response) {
        MessageResp messageResp = userService.logout(response);
        return ResponseEntity.ok(messageResp);
    }

    /* REFRESH TOKEN */
    @PostMapping("/api/refresh")
    public ResponseEntity<MessageResp> refresh(
            @RequestBody(required = false) RefreshTokenDTO refreshTokenDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = refreshTokenDTO != null ? refreshTokenDTO.getRefreshToken() : null;
        MessageResp messageResp = userService.refresh(refreshToken, request, response);
        return ResponseEntity.ok(messageResp);
    }

    /* DELETE TEST USER */
    @PostMapping("/api/delete-test-user")
    public ResponseEntity<MessageResp> deleteTestUser(@RequestBody String login) {
        MessageResp messageResp = userService.deletTestUser(login);
        return ResponseEntity.ok(messageResp);
    }

}
