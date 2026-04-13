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
import com.openclassrooms.etudiant.messages.UserErrorMessage;
import com.openclassrooms.etudiant.messages.UserMessage;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS FOR USER SERVICE
 * Tests business logic for authentication, registration and logout
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtService jwtService;

        @Mock
        private UserDtoMapper userDtoMapper;

        @Mock
        private CustomUserDetailService userDetailsService;

        @Mock
        private HttpServletRequest httpServletRequest;

        @Mock
        private HttpServletResponse httpServletResponse;

        @InjectMocks
        private UserService userService;

        private UserDTO testUserDTO;
        private User testUser;
        private User savedUser;
        private LoginRequestDTO loginRequestDTO;

        @BeforeEach
        void setUp() {
                /* INITIALIZE TEST DATA */
                testUserDTO = UserDTO.builder()
                                .firstName("Jean")
                                .lastName("Dupont")
                                .login("jean.dupont@example.com")
                                .password("Password123!")
                                .build();

                testUser = User.builder()
                                .id(1L)
                                .firstName("Jean")
                                .lastName("Dupont")
                                .login("jean.dupont@example.com")
                                .password("encodedPassword")
                                .build();

                savedUser = testUser;

                loginRequestDTO = new LoginRequestDTO();
                loginRequestDTO.setLogin("jean.dupont@example.com");
                loginRequestDTO.setPassword("Password123!");
                loginRequestDTO.setAuthType(AuthType.HEADER);

                ReflectionTestUtils.setField(userService, "jwtExpirationMs", 3600000L);
                ReflectionTestUtils.setField(userService, "jwtRefreshExpirationMs", 86400000L);
                ReflectionTestUtils.setField(userService, "env", "test");
        }

        /** USER REGISTRATION */
        @Nested
        @DisplayName("User Registration Logic")
        class UserRegistrationTests {

                /* REGISTER SUCCESS */
                @Test
                @DisplayName("Should register user successfully with valid data")
                void shouldRegisterUser_WhenDataIsValid() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(testUserDTO.getLogin())).thenReturn(Optional.empty());
                        when(userDtoMapper.toEntity(testUserDTO)).thenReturn(testUser);
                        when(passwordEncoder.encode(testUserDTO.getPassword())).thenReturn("encodedPassword");
                        when(userRepository.save(any(User.class))).thenReturn(savedUser);

                        /* ACT */
                        MessageResp response = userService.register(testUserDTO);

                        /* ASSERT */
                        assertEquals(UserMessage.REGISTER_SUCCESS.getMessage(), response.getMessage());
                }

                /* REGISTER FAILURE */
                @Test
                @DisplayName("Should throw IllegalStateException when user registration fails")
                void shouldThrowIllegalStateException_WhenUserRegistrationFails() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(testUserDTO.getLogin())).thenReturn(Optional.empty());
                        when(userDtoMapper.toEntity(testUserDTO)).thenReturn(testUser);
                        when(passwordEncoder.encode(testUserDTO.getPassword())).thenReturn("encodedPassword");
                        when(userRepository.save(any(User.class))).thenThrow(IllegalStateException.class);

                        /* ACT & ASSERT */
                        assertThrows(IllegalStateException.class, () -> userService.register(testUserDTO));
                }

                /* REGISTER NULL DTO */
                @Test
                @DisplayName("Should throw IllegalArgumentException when UserDTO is null")
                void shouldThrowIllegalArgumentException_WhenUserDTOIsNull() {
                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class, () -> userService.register(null));
                }

                // : branche Assert.hasText(login) dans register() non
                // couverte
                @Test
                @DisplayName("Should throw IllegalArgumentException when login is blank")
                void shouldThrowIllegalArgumentException_WhenLoginIsBlank() {
                        /* ARRANGE */
                        UserDTO blankLoginDTO = UserDTO.builder()
                                        .firstName("Jean").lastName("Dupont")
                                        .login("").password("Password123!")
                                        .build();

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class, () -> userService.register(blankLoginDTO));
                }

                /* REGISTER DUPLICATE LOGIN */
                @Test
                @DisplayName("Should throw IllegalArgumentException when login already exists")
                void shouldThrowIllegalArgumentException_WhenUserAlreadyExists() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(testUserDTO.getLogin())).thenReturn(Optional.of(testUser));

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class, () -> userService.register(testUserDTO));
                }
        }

        /** USER LOGIN */
        @Nested
        @DisplayName("User Login Logic")
        class UserAuthenticationTests {

                /* LOGIN SUCCESS */
                @Test
                @DisplayName("Should authenticate user successfully with valid credentials")
                void shouldAuthenticateUser_WhenCredentialsAreValid() {

                        /* ARRANGE */
                        when(userRepository.findByLogin(loginRequestDTO.getLogin())).thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtService.generateToken(testUser, false)).thenReturn("mock.jwt.token");
                        when(userDtoMapper.toProfileDto(testUser)).thenReturn(new UserProfileDTO());

                        /* ACT */
                        LoginResponse response = userService.login(loginRequestDTO, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.LOGIN_SUCCESS.getMessage(), response.getMessage());
                        verify(jwtService).generateToken(testUser, false);
                }

                // : branche Assert.hasText(login) dans login() non
                // couverte
                @Test
                @DisplayName("Should throw IllegalArgumentException when login is blank")
                void shouldThrowException_WhenLoginIsBlank() {
                        /* ARRANGE */
                        LoginRequestDTO blankLoginDTO = new LoginRequestDTO();
                        blankLoginDTO.setLogin("");
                        blankLoginDTO.setPassword("Password123!");

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class,
                                        () -> userService.login(blankLoginDTO, httpServletRequest,
                                                        httpServletResponse));
                }

                // : branche Assert.hasText(password) dans login() non
                // couverte
                @Test
                @DisplayName("Should throw IllegalArgumentException when password is blank")
                void shouldThrowException_WhenPasswordIsBlank() {
                        /* ARRANGE */
                        LoginRequestDTO blankPasswordDTO = new LoginRequestDTO();
                        blankPasswordDTO.setLogin("jean.dupont@example.com");
                        blankPasswordDTO.setPassword("");

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class,
                                        () -> userService.login(blankPasswordDTO, httpServletRequest,
                                                        httpServletResponse));
                }

                /* LOGIN INVALID CREDENTIALS */
                @Test
                @DisplayName("Should throw exception when credentials are invalid")
                void shouldThrowException_WhenCredentialsAreInvalid() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class,
                                        () -> userService.login(loginRequestDTO, httpServletRequest,
                                                        httpServletResponse));
                }

                /* LOGIN WRONG PASSWORD */
                @Test
                @DisplayName("Should throw exception when password is wrong")
                void shouldThrowException_WhenPasswordIsWrong() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(loginRequestDTO.getLogin())).thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword()))
                                        .thenReturn(false);

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class,
                                        () -> userService.login(loginRequestDTO, httpServletRequest,
                                                        httpServletResponse));
                }

                /* LOGIN COOKIE AUTH TYPE */
                @Test
                @DisplayName("Should authenticate user and set cookie when auth type is COOKIE")
                void shouldAuthenticateUser_WhenCookieAuthType() {
                        /* ARRANGE - default authType in LoginRequestDTO is COOKIE */
                        LoginRequestDTO cookieLoginDTO = new LoginRequestDTO();
                        cookieLoginDTO.setLogin("jean.dupont@example.com");
                        cookieLoginDTO.setPassword("Password123!");

                        when(userRepository.findByLogin(cookieLoginDTO.getLogin()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(cookieLoginDTO.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtService.generateToken(testUser, false)).thenReturn("mock.jwt.token");
                        when(userDtoMapper.toProfileDto(testUser)).thenReturn(new UserProfileDTO());

                        /* ACT */
                        LoginResponse response = userService.login(cookieLoginDTO, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.LOGIN_SUCCESS.getMessage(), response.getMessage());
                        /* VERIFY cookie header was set (addTokenCookie → addHeader Set-Cookie) */
                        verify(httpServletResponse, atLeastOnce()).addHeader(eq("Set-Cookie"), anyString());
                }

                /* LOGIN REMEMBER ME TRUE - HEADER */
                @Test
                @DisplayName("Should generate refresh token and include it in response when rememberMe=true and HEADER auth")
                void shouldReturnRefreshToken_WhenRememberMeTrue_HeaderAuth() {
                        /* ARRANGE */
                        loginRequestDTO.setRememberMe(true); // authType is already HEADER

                        when(userRepository.findByLogin(loginRequestDTO.getLogin()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtService.generateToken(testUser, false)).thenReturn("mock.access.token");
                        when(jwtService.generateToken(testUser, true)).thenReturn("mock.refresh.token");
                        when(userRepository.save(any(User.class))).thenReturn(testUser);
                        when(userDtoMapper.toProfileDto(testUser)).thenReturn(new UserProfileDTO());

                        /* ACT */
                        LoginResponse response = userService.login(loginRequestDTO, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.LOGIN_SUCCESS.getMessage(), response.getMessage());
                        /* HEADER auth returns refresh token in response body */
                        assertEquals("mock.refresh.token", response.getRefreshToken());
                        verify(jwtService).generateToken(testUser, true);
                        verify(userRepository).save(any(User.class));
                }

                /* LOGIN REMEMBER ME TRUE - COOKIE */
                @Test
                @DisplayName("Should set refresh token cookie and not expose token in body when rememberMe=true and COOKIE auth")
                void shouldSetRefreshCookie_WhenRememberMeTrue_CookieAuth() {
                        /* ARRANGE */
                        LoginRequestDTO cookieRememberMeDTO = new LoginRequestDTO();
                        cookieRememberMeDTO.setLogin("jean.dupont@example.com");
                        cookieRememberMeDTO.setPassword("Password123!");
                        cookieRememberMeDTO.setRememberMe(true);
                        /* authType defaults to COOKIE */

                        when(userRepository.findByLogin(cookieRememberMeDTO.getLogin()))
                                        .thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(cookieRememberMeDTO.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtService.generateToken(testUser, false)).thenReturn("mock.access.token");
                        when(jwtService.generateToken(testUser, true)).thenReturn("mock.refresh.token");
                        when(userRepository.save(any(User.class))).thenReturn(testUser);
                        when(userDtoMapper.toProfileDto(testUser)).thenReturn(new UserProfileDTO());

                        /* ACT */
                        LoginResponse response = userService.login(cookieRememberMeDTO, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.LOGIN_SUCCESS.getMessage(), response.getMessage());
                        /* COOKIE auth does NOT expose the refresh token in the response body */
                        assertNull(response.getRefreshToken());
                        /* Verify refresh token generation happened */
                        verify(jwtService).generateToken(testUser, true);
                }
        }

        /** USER REFRESH TOKEN */
        @Nested
        @DisplayName("User Refresh Token Logic")
        class UserRefreshTokenTests {

                /* REFRESH SUCCESS */
                @Test
                @DisplayName("Should refresh token successfully with valid refresh token")
                void shouldRefreshToken_WhenRefreshTokenIsValid() {
                        /* ARRANGE */
                        String validRefreshToken = "valid.refresh.token";

                        /* COMPUTE SHA-256 HASH TO MATCH validateRefreshToken DB CHECK */
                        try {
                                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                                byte[] hashBytes = digest.digest(validRefreshToken.getBytes(StandardCharsets.UTF_8));
                                testUser.setRefreshToken(HexFormat.of().formatHex(hashBytes));
                        } catch (NoSuchAlgorithmException e) {
                                throw new RuntimeException(e);
                        }

                        when(jwtService.extractUsername(validRefreshToken, true)).thenReturn(testUser.getLogin());
                        when(userDetailsService.loadUserByUsername(testUser.getLogin())).thenReturn(testUser);
                        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
                        when(jwtService.isTokenValid(validRefreshToken, testUser, true)).thenReturn(true);
                        when(jwtService.generateToken(testUser, false)).thenReturn("new.mock.jwt.token");

                        /* ACT */
                        MessageResp response = userService.refresh(validRefreshToken, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.TOKEN_REFRESH_SUCCESS.getMessage(), response.getMessage());
                        verify(jwtService).generateToken(testUser, false);
                }

                // : branche userDetails == null dans refresh() non
                // couverte
                @Test
                @DisplayName("Should return error response when userDetails cannot be loaded")
                void shouldReturnError_WhenUserDetailsIsNull() {
                        /* ARRANGE */
                        String token = "valid.refresh.token";
                        when(jwtService.extractUsername(token, true)).thenReturn(testUser.getLogin());
                        when(userDetailsService.loadUserByUsername(testUser.getLogin())).thenReturn(null);

                        /* ACT */
                        MessageResp response = userService.refresh(token, httpServletRequest, httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserErrorMessage.TOKEN_REFRESH_FAILED.getMessage(), response.getMessage());
                }

                // : branche isTokenValid == false dans refresh() non
                // couverte
                @Test
                @DisplayName("Should return error response when JWT token validation fails")
                void shouldReturnError_WhenTokenValidationFails() {
                        /* ARRANGE */
                        String token = "valid.refresh.token";
                        when(jwtService.extractUsername(token, true)).thenReturn(testUser.getLogin());
                        when(userDetailsService.loadUserByUsername(testUser.getLogin())).thenReturn(testUser);
                        when(jwtService.isTokenValid(token, testUser, true)).thenReturn(false);

                        /* ACT */
                        MessageResp response = userService.refresh(token, httpServletRequest, httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserErrorMessage.TOKEN_REFRESH_FAILED.getMessage(), response.getMessage());
                }

                // : branche storedHashedRefreshToken == null dans
                // validateRefreshToken() non couverte
                @Test
                @DisplayName("Should return error response when no refresh token is stored in database")
                void shouldReturnError_WhenRefreshTokenNotStoredInDb() {
                        /* ARRANGE - testUser has no refreshToken hash stored (null) */
                        String token = "valid.refresh.token";
                        testUser.setRefreshToken(null);
                        when(jwtService.extractUsername(token, true)).thenReturn(testUser.getLogin());
                        when(userDetailsService.loadUserByUsername(testUser.getLogin())).thenReturn(testUser);
                        when(jwtService.isTokenValid(token, testUser, true)).thenReturn(true);
                        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));

                        /* ACT */
                        MessageResp response = userService.refresh(token, httpServletRequest, httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserErrorMessage.TOKEN_REFRESH_FAILED.getMessage(), response.getMessage());
                }

                /* REFRESH INVALID TOKEN */
                @Test
                @DisplayName("Should throw exception when refresh token is invalid")
                void shouldThrowException_WhenRefreshTokenIsInvalid() {
                        /* ARRANGE */
                        String invalidRefreshToken = "invalid.refresh.token";
                        when(jwtService.extractUsername(invalidRefreshToken, true)).thenReturn(null);

                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class,
                                        () -> userService.refresh(invalidRefreshToken, httpServletRequest,
                                                        httpServletResponse));
                }

                /* REFRESH - NO TOKEN ANYWHERE */
                @Test
                @DisplayName("Should return error response when no refresh token is present anywhere")
                void shouldReturnError_WhenNoRefreshTokenPresent() {
                        /* ARRANGE - no body token and empty cookie */
                        when(jwtService.getJwtFromCookies(httpServletRequest, true)).thenReturn(null);

                        /* ACT */
                        MessageResp response = userService.refresh(null, httpServletRequest, httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserErrorMessage.TOKEN_REFRESH_FAILED.getMessage(), response.getMessage());
                }

                /* REFRESH FROM COOKIE */
                @Test
                @DisplayName("Should refresh token successfully when token comes from cookie")
                void shouldRefreshToken_WhenTokenComesFromCookie() {
                        /* ARRANGE */
                        String tokenFromCookie = "token.from.cookie";
                        try {
                                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                                byte[] hashBytes = digest.digest(
                                                tokenFromCookie.getBytes(StandardCharsets.UTF_8));
                                testUser.setRefreshToken(HexFormat.of().formatHex(hashBytes));
                        } catch (java.security.NoSuchAlgorithmException e) {
                                throw new RuntimeException(e);
                        }
                        when(jwtService.getJwtFromCookies(httpServletRequest, true)).thenReturn(tokenFromCookie);
                        when(jwtService.extractUsername(tokenFromCookie, true)).thenReturn(testUser.getLogin());
                        when(userDetailsService.loadUserByUsername(testUser.getLogin())).thenReturn(testUser);
                        when(jwtService.isTokenValid(tokenFromCookie, testUser, true)).thenReturn(true);
                        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
                        when(jwtService.generateToken(testUser, false)).thenReturn("new.access.token");

                        /* ACT */
                        MessageResp response = userService.refresh(null, httpServletRequest, httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.TOKEN_REFRESH_SUCCESS.getMessage(), response.getMessage());
                        verify(jwtService).generateToken(testUser, false);
                }

        }

        /** USER LOGOUT */
        @Nested
        @DisplayName("User Logout Logic")
        class UserLogoutTests {

                /* LOGOUT SUCCESS */
                @Test
                @DisplayName("Should logout user successfully")
                void shouldLogoutUser_Successfully() {
                        /* ACT */
                        MessageResp response = userService.logout(httpServletResponse);

                        /* ASSERT */
                        assertEquals(UserMessage.LOGOUT_SUCCESS.getMessage(), response.getMessage());

                        /* VERIFY TWO COOKIES DELETIONS (token and refreshToken) */
                        verify(httpServletResponse, times(2)).addHeader(eq("Set-Cookie"), anyString());
                }
        }

    /** USER ERROR MESSAGE ENUM */
    @Nested
    @DisplayName("UserErrorMessage - Enum messages")
    class UserErrorMessageTests {

        /* GET MESSAGE */
        @Test
        @DisplayName("Should return non-blank message for every enum value")
        void shouldReturnNonBlankMessage_ForEveryValue() {
            /* ACT & ASSERT */
            for (UserErrorMessage msg : UserErrorMessage.values()) {
                assertNotNull(msg.getMessage());
                assertFalse(msg.getMessage().isBlank());
            }
        }

        /* SPECIFIC VALUES */
        @Test
        @DisplayName("Should return expected message for INVALID_CREDENTIALS")
        void shouldReturnExpectedMessage_ForInvalidCredentials() {
            /* ASSERT */
            assertEquals("Invalid login credentials", UserErrorMessage.INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("Should return expected message for USER_NOT_FOUND")
        void shouldReturnExpectedMessage_ForUserNotFound() {
            /* ASSERT */
            assertEquals("User not found", UserErrorMessage.USER_NOT_FOUND.getMessage());
        }
    }

    /** USER MESSAGE ENUM */
    @Nested
    @DisplayName("UserMessage - Enum messages")
    class UserMessageTests {

        /* GET MESSAGE */
        @Test
        @DisplayName("Should return non-blank message for every enum value")
        void shouldReturnNonBlankMessage_ForEveryValue() {
            /* ACT & ASSERT */
            for (UserMessage msg : UserMessage.values()) {
                assertNotNull(msg.getMessage());
                assertFalse(msg.getMessage().isBlank());
            }
        }

        /* SPECIFIC VALUES */
        @Test
        @DisplayName("Should return expected message for LOGIN_SUCCESS")
        void shouldReturnExpectedMessage_ForLoginSuccess() {
            /* ASSERT */
            assertEquals("Logged in successfully", UserMessage.LOGIN_SUCCESS.getMessage());
        }

        @Test
        @DisplayName("Should return expected message for LOGOUT_SUCCESS")
        void shouldReturnExpectedMessage_ForLogoutSuccess() {
            /* ASSERT */
            assertEquals("User logged out successfully", UserMessage.LOGOUT_SUCCESS.getMessage());
        }
    }

}