package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;
import com.openclassrooms.etudiant.dto.dtoHelpers.LoginResponse;
import com.openclassrooms.etudiant.dto.dtoHelpers.MessageResp;
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
import org.springframework.http.ResponseEntity;
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
                /* INITIALIZE MOCKMVC WITHOUT SPRING CONTEXT */
                mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
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

        @Nested
        @DisplayName("POST /api/register - User Registration")
        class UserRegistrationEndpoint {

                @Test
                @DisplayName("Should register user successfully with valid data")
                void shouldRegisterUser_WhenDataIsValid() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        MessageResp successResponse = MessageResp.success("User registered successfully");
                        when(userService.register(any(UserDTO.class)))
                                        .thenReturn(ResponseEntity.ok(successResponse));

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(testUserDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("User registered successfully"));
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
        }

        @Nested
        @DisplayName("POST /api/login - User Authentication")
        class UserAuthenticationEndpoint {

                @Test
                @DisplayName("Should authenticate user successfully with valid credentials")
                void shouldAuthenticateUser_WhenCredentialsAreValid() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        LoginResponse successResponse = LoginResponse.success("Logged in successfully", null,
                                        AuthType.COOKIE,
                                        null);
                        when(userService.login(any(LoginRequestDTO.class), any(), any()))
                                        .thenReturn(ResponseEntity.ok(successResponse));

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(loginRequestDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logged in successfully"));
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
        }

        @Nested
        @DisplayName("POST /api/logout - User Logout")
        class UserLogoutEndpoint {

                @Test
                @DisplayName("Should logout user successfully")
                void shouldLogoutUser_Successfully() throws Exception {
                        /* ARRANGE MOCK RESPONSE */
                        MessageResp successResponse = MessageResp.success("User logged out successfully");
                        when(userService.logout(any()))
                                        .thenReturn(ResponseEntity.ok(successResponse));

                        /* ACT AND ASSERT 200 OK */
                        mockMvc.perform(post("/api/logout"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("User logged out successfully"));
                }
        }
}