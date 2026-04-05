package com.openclassrooms.etudiant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION TESTS FOR USER CONTROLLER
 * Boots the full Spring context with an H2 in-memory database (profile "test").
 * Tests end-to-end HTTP → Service → Repository flow, including Spring Security.
 * ./mvnw clean test -Dtest=UserIntegrationTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Integration Tests")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    /** SETUP METHOD */
    /* CLEAN DATABASE BEFORE EACH TEST */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    /** USER REGISTRATION */
    @Nested
    @DisplayName("POST /api/register - User Registration")
    class UserRegistrationEndpoint {

        /* REGISTER SUCCESS */
        @Test
        @DisplayName("Should register user successfully and persist to database")
        void shouldRegisterUser_WhenDataIsValid() throws Exception {
            /* ARRANGE */
            UserDTO newUser = UserDTO.builder()
                    .firstName("Loraine")
                    .lastName("Pierson")
                    .login("integration@test.com")
                    .password("Password123!")
                    .build();

            /* ACT AND ASSERT 200 OK */
            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            /* ASSERT DATABASE STATE */
            assertEquals(1, userRepository.count());
        }

        /* REGISTER DUPLICATE */
        @Test
        @DisplayName("Should return 400 when login already exists")
        void shouldReturn400_WhenLoginAlreadyExists() throws Exception {
            /* ARRANGE - register the same user twice */
            UserDTO newUser = UserDTO.builder()
                    .firstName("Loraine")
                    .lastName("Pierson")
                    .login("duplicate@test.com")
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk());

            /* ACT AND ASSERT 400 BAD REQUEST ON SECOND REGISTRATION */
            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));

            /* ASSERT ONLY ONE USER WAS PERSISTED */
            assertEquals(1, userRepository.count());
        }

        /* REGISTER VALIDATION FAILURE */
        @Test
        @DisplayName("Should return 400 when request body fails validation")
        void shouldReturn400_WhenValidationFails() throws Exception {
            /* ARRANGE - invalid email and short password */
            UserDTO invalidUser = UserDTO.builder()
                    .firstName("Loraine")
                    .lastName("Pierson")
                    .login("not-an-email")
                    .password("short")
                    .build();

            /* ACT AND ASSERT 400 BAD REQUEST */
            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidUser)))
                    .andExpect(status().isBadRequest());
        }
    }

    /** USER AUTHENTICATION */
    @Nested
    @DisplayName("POST /api/login - User Authentication")
    class UserAuthenticationEndpoint {

        /* LOGIN SUCCESS */
        @Test
        @DisplayName("Should login successfully and return success response")
        void shouldLoginUser_WhenCredentialsAreValid() throws Exception {
            /* ARRANGE - first register a user */
            UserDTO newUser = UserDTO.builder()
                    .firstName("Loraine")
                    .lastName("Pierson")
                    .login("login@test.com")
                    .password("Password123!")
                    .build();
            mockMvc.perform(post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)));

            LoginRequestDTO loginRequest = new LoginRequestDTO();
            loginRequest.setLogin("login@test.com");
            loginRequest.setPassword("Password123!");
            loginRequest.setAuthType(AuthType.HEADER);

            /* ACT AND ASSERT 200 OK WITH JWT IN AUTHORIZATION HEADER */
            mockMvc.perform(post("/api/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged in successfully"));
        }
    }

    /** SPRING SECURITY - UNAUTHENTICATED ACCESS */
    @Nested
    @DisplayName("Security - Protected endpoint access")
    class SecurityTests {

        /* NO TOKEN → 401 */
        @Test
        @DisplayName("Should return 401 when accessing protected route without token")
        void shouldReturn401_WhenNoTokenProvided() throws Exception {
            /* ACT AND ASSERT 401 UNAUTHORIZED on protected resource */
            mockMvc.perform(get("/api/students"))
                    .andExpect(status().isUnauthorized());
        }
    }
}