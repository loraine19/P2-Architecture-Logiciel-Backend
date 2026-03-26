package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.AuthDTO;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/api/login")
    // added @RequestBody to login method
    // added return type ResponseEntity<?> to login method & DTO AuthDTO
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletResponse response) {
        AuthDTO jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        @SuppressWarnings("null")
        ResponseCookie jwtCookie = ResponseCookie.from("token", jwtToken.getToken())
                .httpOnly(true) // (protect XSS)
                .secure(false) // pass to HTTPS (in Prod)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new AuthDTO(jwtToken.getIsAuthenticated(), jwtToken.getPartialToken()));
    }

}
