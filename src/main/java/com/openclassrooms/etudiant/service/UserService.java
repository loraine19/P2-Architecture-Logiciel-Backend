package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.configuration.security.CustomUserDetailService;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.UserProfileDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;
import com.openclassrooms.etudiant.dto.dtoHelpers.LoginResponse;
import com.openclassrooms.etudiant.dto.dtoHelpers.MessageResp;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.interfaces.UserServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
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
public class UserService implements UserServiceImpl {
    // Dependencies
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDtoMapper userDtoMapper;
    private final CustomUserDetailService userDetailsService;

    // Configuration from environment
    @Value("${ENV:dev}")
    private String env;

    @Value("${JWT_EXPIRATION_MS:3600000}")
    private Long jwtExpirationMs;

    @Value("${JWT_REFRESH_EXPIRATION_MS:172800000}")
    private Long jwtRefreshExpirationMs;

    /** PUBLIC SERVICE METHODS */

    /* REGISTER NEW USER */
    @Override
    public MessageResp register(UserDTO userDTO) {
        Assert.notNull(userDTO, "User data cannot be null");
        Assert.hasText(userDTO.getLogin(), "User login cannot be empty");

        // Check for existing user
        if (userRepository.findByLogin(userDTO.getLogin()).isPresent())
            throw new IllegalArgumentException("Login already exists");

        // Map DTO to entity and encrypt password
        User newUser = userDtoMapper.toEntity(userDTO);
        newUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        // Save to database
        User savedUser = userRepository.save(newUser);
        if (savedUser == null || savedUser.getId() == null)
            throw new IllegalStateException("User registration failed");
        return MessageResp.success("User registered successfully");
    }

    /* LOGIN USER */
    @Override
    public LoginResponse login(
            LoginRequestDTO loginRequestDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(loginRequestDTO, "Login request cannot be null");
        Assert.hasText(loginRequestDTO.getLogin(), "Login cannot be empty");
        Assert.hasText(loginRequestDTO.getPassword(), "Password cannot be empty");

        // Find and validate user
        Optional<User> userOptional = userRepository.findByLogin(loginRequestDTO.getLogin());
        if (userOptional.isEmpty())
            throw new IllegalArgumentException("Invalid login credentials");

        User user = userOptional.get();

        // Verify password
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword()))
            throw new IllegalArgumentException("Invalid login credentials");

        // Generate JWT token
        String jwtToken = jwtService.generateToken(user, false);

        // Determine token destination
        AuthType authType = loginRequestDTO.getAuthType() != null ? loginRequestDTO.getAuthType() : AuthType.COOKIE;

        // Generate refresh token if rememberMe is true
        String refreshToken = loginRequestDTO.isRememberMe() ? jwtService.generateToken(user, true) : null;

        // if rememberMe is true, set hash refresh token in Database for future
        if (refreshToken != null) {
            String hashedRefreshToken = hashWithSHA256(refreshToken);
            user.setRefreshToken(hashedRefreshToken);
            userRepository.save(user);
        }

        // Set cookie or header based on auth type
        if (authType == AuthType.COOKIE)
            setAuthCookie(response, jwtToken,
                    loginRequestDTO.isRememberMe(), refreshToken, false);
        else
            injectTokenIntoHeaders(request, response, jwtToken);

        // Create user profile for response (without password)
        UserProfileDTO userProfile = userDtoMapper.toProfileDto(user);

        // Include refresh token in response only for header-based auth (mobile)
        String responseRefreshToken = (authType == AuthType.HEADER) ? refreshToken : null;

        LoginResponse successResponse = LoginResponse.success(
                "Logged in successfully",
                userProfile,
                authType,
                responseRefreshToken);
        return successResponse;
    }

    /* LOGOUT USER */
    @Override
    public MessageResp logout(HttpServletResponse response) {
        // Create cookie deletion response
        setAuthCookie(response, "", true, "", true);
        MessageResp logoutMessage = MessageResp.success("User logged out successfully");
        return logoutMessage;
    }

    /* REFRESH TOKEN */
    @Override
    public MessageResp refresh(String refreshToken, HttpServletRequest request,
            HttpServletResponse response) {

        AuthType authType = AuthType.HEADER;
        MessageResp errorResponse = MessageResp.error("Token refresh failed");

        // Extract refresh token from cookies if not in body
        if (refreshToken == null) {
            refreshToken = jwtService.getJwtFromCookies(request, true);
            if (refreshToken == null)
                return errorResponse;
            else
                authType = AuthType.COOKIE;
        }

        String userLogin = this.jwtService.extractUsername(refreshToken, true);
        if (userLogin == null)
            return errorResponse;
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userLogin);
        if (userDetails == null)
            return errorResponse;
        Boolean isValid = this.jwtService.isTokenValid(refreshToken, userDetails, true);
        if (!isValid)
            return errorResponse;
        Boolean isValidDB = validateRefreshToken(refreshToken);
        if (!isValidDB)
            return errorResponse;

        String newAccessToken = jwtService.generateToken(userDetails, false);

        if (authType == AuthType.COOKIE)
            addTokenCookie(response, newAccessToken, jwtExpirationMs, "token");

        else
            injectTokenIntoHeaders(request, response, newAccessToken);

        return MessageResp.success("Token refreshed successfully");
    }

    /** PRIVATE HELPER METHODS */

    /* ADD TOKEN COOKIE */
    private void addTokenCookie(HttpServletResponse response, String token, Long expirationMS,
            String name) {
        ResponseCookie tokenCookie = ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure("prod".equalsIgnoreCase(env))
                .path("/")
                .maxAge(expirationMS / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", tokenCookie.toString());
    }

    /* VALIDATE REFRESH TOKEN */
    private boolean validateRefreshToken(String refreshToken) {
        String userLogin = jwtService.extractUsername(refreshToken, true);
        if (userLogin == null)
            return errorFalseReturn("No userLogin");

        UserDetails userDetails = userDetailsService.loadUserByUsername(userLogin);
        if (userDetails == null)
            return errorFalseReturn("No userDetails");

        Boolean isValid = jwtService.isTokenValid(refreshToken, userDetails, true);
        if (!isValid)
            return errorFalseReturn("Token not valid");

        Optional<User> userOptional = userRepository.findByLogin(userLogin);
        if (userOptional.isEmpty())
            return errorFalseReturn("User not found");

        User user = userOptional.get();
        String storedHashedRefreshToken = user.getRefreshToken();

        if (storedHashedRefreshToken == null || storedHashedRefreshToken.isEmpty())
            return errorFalseReturn("No refresh token in DB");

        String incomingTokenHash = hashWithSHA256(refreshToken);
        boolean isValidDB = storedHashedRefreshToken.equals(incomingTokenHash);

        if (!isValidDB)
            return errorFalseReturn("Refresh token hash mismatch");

        return isValidDB;
    }

    /* SET AUTH COOKIE */
    private void setAuthCookie(HttpServletResponse response, String token,
            @Nullable Boolean remenberMe, @Nullable String refreshToken, @Nullable Boolean isLogout) {

        addTokenCookie(response, token, isLogout ? 0 : jwtExpirationMs, "token");
        if (refreshToken != null)
            addTokenCookie(response, refreshToken, isLogout ? 0 : jwtRefreshExpirationMs, "refreshToken");

    }

    /* INJECT TOKEN INTO HEADERS FOR MOBILE CLIENTS */
    private void injectTokenIntoHeaders(HttpServletRequest request, HttpServletResponse response, String token) {
        response.setHeader("Authorization", "Bearer " + token);
    }

    /* HASH WITH SHA-256 FOR REFRESH TOKEN */
    private String hashWithSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandatory in any JVM", e);
        }
    }

    /* ERRORS HELPERS */
    private Boolean errorFalseReturn(String message) {
        log.warn(message);
        return false;
    }

}
