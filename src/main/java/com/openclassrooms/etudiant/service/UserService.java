package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.entities.Auth;
import com.openclassrooms.etudiant.entities.MessageResp;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.Interfaces.UserServiceInterface;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserServiceInterface {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Injected from .env
    @org.springframework.beans.factory.annotation.Value("${ENV}")
    private String env;
    @org.springframework.beans.factory.annotation.Value("${JWT_EXPIRATION_MS}")
    private Long jwtExpirationMs;

    public MessageResp register(UserDTO user) {
        Assert.notNull(user, "UserDTO must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
        if (optionalUser.isPresent()) {
            throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
        }
        User newUser = new User();
        newUser.setLogin(user.getLogin());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        userRepository.save(newUser);
        return new MessageResp(user.getLogin() + " registered successfully");

    }

    public void setAuthCookie(HttpServletResponse response, String token) {
        long durationInSeconds = jwtExpirationMs / 1000;
        ResponseCookie jwtCookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(env.equals("prod"))
                .path("/")
                .maxAge(durationInSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", jwtCookie.toString());
    }

    public MessageResp login(LoginRequestDTO loginRequestDTO, HttpServletResponse response) {
        System.out.println("userService data: " + loginRequestDTO.getLogin() + " " + loginRequestDTO.getPassword());
        Assert.notNull(loginRequestDTO.getLogin(), "Login must not be null..");
        Assert.notNull(loginRequestDTO.getPassword(), "Password must not be null");
        // Find user by login
        Optional<User> user = userRepository.findByLogin(loginRequestDTO.getLogin());
        log.info("User found: " + user);
        /// Password verification
        if (user.isPresent() && passwordEncoder.matches(loginRequestDTO.getPassword(), user.get().getPassword())) {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.get().getLogin())
                    .password(user.get().getPassword())
                    .authorities("USER")
                    .build();

            /// JWT generation
            Auth jwtToken = new Auth(true, jwtService.generateToken(userDetails));

            /// Create secure cookie with JWT token
            setAuthCookie(response, jwtToken.getToken());

            return new MessageResp("Login successful");

        } else {
            throw new IllegalArgumentException("Invalid credentials");
        }

    }

    public ResponseEntity<Void> logout(HttpServletResponse response) {

        ResponseCookie deleteCookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(env.equals("prod"))
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", deleteCookie.toString())
                .build();
    }

}
