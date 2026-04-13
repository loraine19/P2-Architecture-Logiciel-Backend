package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;
import com.openclassrooms.etudiant.dto.dtoHelpers.LoginResponse;
import com.openclassrooms.etudiant.dto.dtoHelpers.MessageResp;
import com.openclassrooms.etudiant.exception.GlobalExceptionHandler;
import com.openclassrooms.etudiant.messages.UserErrorMessage;
import com.openclassrooms.etudiant.messages.UserMessage;
import com.openclassrooms.etudiant.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UNIT TESTS FOR USER CONTROLLER
 * Uses StandaloneSetup to completely bypass Spring Context and Security issues
 * ./mvnw clean test -Dtest=UserControllerTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @Mock
        private UserService userService;

        @InjectMocks
        private UserController userController;

        private UserDTO testUserDTO;
        private LoginRequestDTO loginRequestDTO;

        @BeforeEach
        void setUp() {
                /* INITIALIZE MOCKMVC WITHOUT SPRING CONTEXT + GLOBAL EXCEPTION HANDLER */
                mockMvc = MockMvcBuilders.standaloneSetup(userController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
                objectMapper = new ObjectMapper();

                /* INITIALIZE VALID TEST DATA */
                testUserDTO = UserDTO.builder()
                                .firstName("Jean")
                                .lastName("Dupont")
                                .login("jean.dupont@example.com")
                                .password("Password123!")
                                .build();

                loginRequestDTO = new LoginRequestDTO();
                loginRequestDTO.setLogin("jean.dupont@example.com");
                loginRequestDTO.setPassword("Password123!");
                loginRequestDTO.setAuthType(AuthType.COOKIE);
        }

        /** USER REGISTRATION */
        @Nested
        @DisplayName("POST /api/register - User Registration")
        class UserRegistrationEndpoint {

                @Test
                @DisplayName("Should register user successfully with valid data")
                void shouldRegisterUser_WhenDataIsValid() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        MessageResp successResponse = MessageResp
                                        .success(UserMessage.REGISTER_SUCCESS.getMessage());
                        when(userService.register(any(UserDTO.class)))
                                        .thenReturn(successResponse);

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(testUserDTO)))

                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message")
                                                        .value(UserMessage.REGISTER_SUCCESS.getMessage()));
                }

                @Test
                @DisplayName("Should return 400 when validation fails")
                void shouldReturn400_WhenValidationFails() throws Exception {
                        /* ARRANGE INVALID DTO */
                        UserDTO invalidUserDTO = UserDTO.builder()
                                        .firstName("Jean")
                                        .lastName("Dupont")
                                        .login("invalid-email")
                                        .password("short")
                                        .build();

                        /* ACT AND ASSERT 400 BAD REQUEST */
                        mockMvc.perform(post("/api/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 when login already exists")
                void shouldReturn400_WhenLoginAlreadyExists() throws Exception {
                        /* ARRANGE - SERVICE THROWS DUPLICATE LOGIN */
                        when(userService.register(any(UserDTO.class)))
                                        .thenThrow(new IllegalArgumentException("Login already exists"));

                        /* ACT AND ASSERT 400 BAD REQUEST */
                        mockMvc.perform(post("/api/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(testUserDTO)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
                }
        }

        /** USER AUTHENTICATION */
        @Nested
        @DisplayName("POST /api/login - User Authentication")
        class UserAuthenticationEndpoint {

                @Test
                @DisplayName("Should authenticate user successfully with valid credentials")
                void shouldAuthenticateUser_WhenCredentialsAreValid() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        LoginResponse successResponse = LoginResponse.success(
                                        UserMessage.LOGIN_SUCCESS.getMessage(), null,
                                        AuthType.COOKIE,
                                        null);
                        when(userService.login(any(LoginRequestDTO.class), any(), any()))
                                        .thenReturn(successResponse);

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(loginRequestDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message")
                                                        .value(UserMessage.LOGIN_SUCCESS.getMessage()));
                }

                @Test
                @DisplayName("Should return 400 when validation fails")
                void shouldReturn400_WhenValidationFails() throws Exception {
                        /* ARRANGE INVALID DTO */
                        LoginRequestDTO invalidLoginDTO = new LoginRequestDTO();
                        invalidLoginDTO.setLogin("");
                        invalidLoginDTO.setPassword("");

                        /* ACT AND ASSERT 400 BAD REQUEST */
                        mockMvc.perform(post("/api/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(invalidLoginDTO)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 when credentials are invalid")
                void shouldReturn400_WhenCredentialsAreInvalid() throws Exception {
                        /* ARRANGE - SERVICE THROWS ON INVALID CREDENTIALS */
                        when(userService.login(any(LoginRequestDTO.class), any(), any()))
                                        .thenThrow(new IllegalArgumentException("Invalid login credentials"));

                        /* ACT AND ASSERT 400 BAD REQUEST */
                        mockMvc.perform(post("/api/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(loginRequestDTO)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
                }
        }

        /** USER REFRESH TOKEN */
        @Nested
        @DisplayName("POST /api/refresh - User Refresh Token")
        class UserRefreshTokenEndpoint {

                @Test
                @DisplayName("Should refresh token successfully with valid refresh token")
                void shouldRefreshToken_WhenRefreshTokenIsValid() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        MessageResp successResponse = MessageResp
                                        .success(UserMessage.TOKEN_REFRESH_SUCCESS.getMessage());
                        when(userService.refresh(any(), any(), any()))
                                        .thenReturn(successResponse);

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString("valid.refresh.token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message")
                                                        .value(UserMessage.TOKEN_REFRESH_SUCCESS.getMessage()));
                }

                @Test
                @DisplayName("Should return 400 when refresh token is invalid")
                void shouldReturn400_WhenRefreshTokenIsInvalid() throws Exception {
                        /* ARRANGE - SERVICE THROWS ON INVALID TOKEN */
                        when(userService.refresh(any(), any(), any()))
                                        .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

                        /* ACT AND ASSERT 400 BAD REQUEST */
                        mockMvc.perform(post("/api/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString("invalid.refresh.token")))
                                        .andExpect(status().isBadRequest());
                }

        }

        /** USER LOGOUT */
        @Nested
        @DisplayName("POST /api/logout - User Logout")
        class UserLogoutEndpoint {

                @Test
                @DisplayName("Should logout user successfully")
                void shouldLogoutUser_Successfully() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        MessageResp successResponse = MessageResp
                                        .success(UserMessage.LOGOUT_SUCCESS.getMessage());
                        when(userService.logout(any()))
                                        .thenReturn(successResponse);

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/logout"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message")
                                                        .value(UserMessage.LOGOUT_SUCCESS.getMessage()));
                }
        }
}