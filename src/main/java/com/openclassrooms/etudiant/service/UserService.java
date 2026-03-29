package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.configuration.security.JwtAuthenticationFilter;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.LoginResponse;
import com.openclassrooms.etudiant.dto.MessageResp;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.UserProfileDTO;
import com.openclassrooms.etudiant.entities.Auth;
import com.openclassrooms.etudiant.entities.AuthType;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.interfaces.UserServiceInterface;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * User authentication and management service
 * Handles registration, login/logout, and session management
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserServiceInterface {
    // Dependencies
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDtoMapper userDtoMapper;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Configuration from environment
    @Value("${ENV:dev}")
    private String env;

    @Value("${JWT_EXPIRATION_MS:3600000}")
    private Long jwtExpirationMs;

    @Value("${JWT_REFRESH_EXPIRATION_MS:172800000}")
    private Long jwtRefreshExpirationMs;

    // Register new user account
    @Override
    public MessageResp register(UserDTO userDTO) {
        Assert.notNull(userDTO, "User data cannot be null");
        Assert.hasText(userDTO.getLogin(), "User login cannot be empty");

        log.debug("Registration attempt for user: {}", userDTO.getLogin());

        // Check for existing user
        if (userRepository.findByLogin(userDTO.getLogin()).isPresent()) {
            log.warn("Registration failed - user already exists: {}", userDTO.getLogin());
            throw new IllegalArgumentException("User with login '" + userDTO.getLogin() + "' already exists");
        }

        try {
            // Map DTO to entity and encrypt password
            User newUser = userDtoMapper.toEntity(userDTO);
            newUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));

            // Save to database
            User savedUser = userRepository.save(newUser);
            log.info("User registered successfully: {} (ID: {})", savedUser.getLogin(), savedUser.getId());

            return MessageResp.success("User '" + userDTO.getLogin() + "' registered successfully");

        } catch (DataIntegrityViolationException e) {
            log.error("Database constraint violation during registration for user: {}", userDTO.getLogin(), e);
            throw new IllegalArgumentException("Registration failed due to data constraint violation");
        }
    }

    // Authenticate user credentials and create session
    @Override
    public LoginResponse login(
            LoginRequestDTO loginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(loginRequestDTO, "Login request cannot be null");
        Assert.hasText(loginRequestDTO.getLogin(), "Login cannot be empty");
        Assert.hasText(loginRequestDTO.getPassword(), "Password cannot be empty");

        log.debug("Login attempt for user: {}", loginRequestDTO.getLogin());

        // Find and validate user
        Optional<User> userOptional = userRepository.findByLogin(loginRequestDTO.getLogin());
        if (userOptional.isEmpty()) {
            log.warn("Login failed - user not found: {}", loginRequestDTO.getLogin());
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = userOptional.get();

        // Verify password
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", loginRequestDTO.getLogin());
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Generate JWT token
        Auth jwtAuth = new Auth(true, jwtService.generateToken(user));

        // Determine token destination
        AuthType authType = loginRequestDTO.getAuthType() != null ? loginRequestDTO.getAuthType() : AuthType.COOKIE;

        String refreshToken = loginRequestDTO.isRememberMe() ? jwtService.generateRefreshToken(user) : null;

        // if rememberMe is true, set hash refresh token in Database for future
        // validation
        if (refreshToken != null) {
            String hashedRefreshToken = hashWithSHA256(refreshToken);
            user.setRefreshToken(hashedRefreshToken);
            userRepository.save(user);
        }

        // Set cookie or header based on auth type
        if (authType == AuthType.COOKIE) {
            setAuthCookie(response, jwtAuth.getToken(), String.valueOf(jwtExpirationMs / 1000),
                    loginRequestDTO.isRememberMe(), refreshToken, String.valueOf(jwtRefreshExpirationMs / 1000));
        } else {
            injectTokenIntoHeaders(request, response, jwtAuth.getToken());
        }

        // Create user profile for response (without password)
        UserProfileDTO userProfile = userDtoMapper.toProfileDto(user);

        log.info("User logged in successfully: {}", user.getLogin());

        // Include refresh token in response only for header-based auth (mobile)
        String responseRefreshToken = (authType == AuthType.HEADER) ? refreshToken : null;

        LoginResponse successResponse = LoginResponse.success(
                "' logged in successfully",
                userProfile,
                authType,
                responseRefreshToken);
        return successResponse;
    }

    // Logout user by clearing authentication cookie
    @Override
    public ResponseEntity<MessageResp> logout(HttpServletResponse response) {
        log.debug("User logout request received");

        // Create cookie deletion response
        setAuthCookie(response, "", "0", true, "", "0");

        MessageResp logoutMessage = MessageResp.success("User logged out successfully");
        return ResponseEntity.ok(logoutMessage);
    }

    @Override
    public ResponseEntity<MessageResp> refresh(String refreshToken, HttpServletRequest request,
            HttpServletResponse response) {

        // Extract refresh token from cookies
        System.out.println("Received refresh token: " + refreshToken);
        if (refreshToken == null)
            return ResponseEntity.badRequest().body(MessageResp.error("No refresh token provided"));

        // Validate refresh token and generate new access token
        // (Implementation would go here, including hashing the provided refresh token
        // and comparing with stored hash)

        this.jwtAuthenticationFilter.validateRefreshToken(refreshToken);
        String userLogin = jwtService.extractUsername(refreshToken, true);
        Optional<User> userOptional = userRepository.findByLogin(userLogin);
        User user = userOptional.get();
        String newAccessToken = jwtService.generateToken(user);

        injectTokenIntoHeaders(request, response, newAccessToken);
        System.out.println(response.getHeader("Authorization"));
        return ResponseEntity.ok(MessageResp.success("Access token refreshed successfully" + newAccessToken));
    }

    /** PRIVATE HELPER METHODS */

    // Set secure HTTP-only authentication cookie
    @SuppressWarnings("null")
    private void setAuthCookie(HttpServletResponse response, String token, String durationInSeconds,
            @Nullable Boolean remenberMe, @Nullable String refreshToken, @Nullable String refreshDurationInSeconde) {

        ResponseCookie jwtCookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure("prod".equalsIgnoreCase(env))
                .path("/")
                .maxAge(Long.parseLong(durationInSeconds))
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", jwtCookie.toString());

        if (refreshToken != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure("prod".equalsIgnoreCase(env))
                    .path("/")
                    .maxAge(jwtRefreshExpirationMs)
                    .sameSite("Lax")
                    .build();
            response.addHeader("Set-Cookie", refreshCookie.toString());
        }

        log.debug("Authentication cookie set with expiration: {} seconds", durationInSeconds);
    }

    // Inject token into HTTP headers for mobile clients
    private void injectTokenIntoHeaders(HttpServletRequest request, HttpServletResponse response, String token) {
        response.setHeader("Authorization", "Bearer " + token);
    }

    // Hash refresh token with SHA-256 (doesn't have BCrypt's 72-byte limitation)
    private String hashWithSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

}
