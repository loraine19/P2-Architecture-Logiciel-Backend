package com.openclassrooms.etudiant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;
import com.openclassrooms.etudiant.repository.StudentRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION TESTS FOR STUDENT CONTROLLER
 * Boots the full Spring context with an H2 in-memory database (profile "test").
 * Tests the complete HTTP → Security → Controller → Service → Repository chain.
 * Verifies both authenticated (JWT) and unauthenticated access patterns.
 * ./mvnw clean test -Dtest=StudentIntegrationTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Student Integration Tests")
class StudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    /* TEST USER CREDENTIALS - reused across all tests */
    private static final String TEST_LOGIN = "student.test@example.com";
    private static final String TEST_PASSWORD = "Password123!";

    /** SETUP METHOD */
    /* CLEAN BOTH TABLES BEFORE EACH TEST AND REGISTER A FRESH TEST USER */
    @BeforeEach
    void setUp() throws Exception {
        studentRepository.deleteAll();
        userRepository.deleteAll();

        /* REGISTER TEST USER SO LOGIN IS ALWAYS AVAILABLE */
        UserDTO testUser = UserDTO.builder()
                .firstName("Test")
                .lastName("User")
                .login(TEST_LOGIN)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)));
    }

    /*
     * HELPER - LOGIN AND RETURN FULL AUTHORIZATION HEADER VALUE (e.g. "Bearer xxx")
     */
    private String getAuthorizationHeader() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setLogin(TEST_LOGIN);
        loginRequest.setPassword(TEST_PASSWORD);
        /* USE HEADER AUTH TYPE SO JWT IS RETURNED IN RESPONSE HEADER */
        loginRequest.setAuthType(AuthType.HEADER);

        MvcResult result = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getHeader("Authorization");
    }

    /* HELPER - BUILD A VALID STUDENT DTO WITH REQUIRED FIELDS */
    private StudentDTO buildValidStudent(String email) {
        return StudentDTO.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .email(email)
                .address("12 rue de la Paix")
                .city("Paris")
                .zipCode("75001")
                .build();
    }

    /** GET ALL STUDENTS */
    @Nested
    @DisplayName("GET /api/students - Get All Students")
    class GetAllStudentsEndpoint {

        /* NO TOKEN → 401 */
        @Test
        @DisplayName("Should return 401 when no authentication token is provided")
        void shouldReturn401_WhenNoTokenProvided() throws Exception {
            /* ACT AND ASSERT 401 UNAUTHORIZED - security filter rejects the request */
            mockMvc.perform(get("/api/students"))
                    .andExpect(status().isUnauthorized());
        }

        /* WITH VALID JWT → 200 */
        @Test
        @DisplayName("Should return empty list when no students exist")
        void shouldReturnEmptyList_WhenNoStudentsExist() throws Exception {
            /* ARRANGE */
            String token = getAuthorizationHeader();

            /* ACT AND ASSERT 200 OK WITH AN EMPTY ARRAY */
            mockMvc.perform(get("/api/students")
                    .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    /** CREATE STUDENT */
    @Nested
    @DisplayName("POST /api/students - Create Student")
    class CreateStudentEndpoint {

        /* CREATE WITH JWT → 201 */
        @Test
        @DisplayName("Should create student successfully with valid token and data")
        void shouldCreateStudent_WhenDataIsValid() throws Exception {
            /* ARRANGE */
            String token = getAuthorizationHeader();
            StudentDTO newStudent = buildValidStudent("jean.dupont@example.com");

            /* ACT AND ASSERT 201 CREATED WITH STUDENT FIELDS IN RESPONSE */
            mockMvc.perform(post("/api/students")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newStudent)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstName").value("Jean"))
                    .andExpect(jsonPath("$.lastName").value("Dupont"))
                    .andExpect(jsonPath("$.email").value("jean.dupont@example.com"));

            /* ASSERT DATABASE STATE - one student persisted */
            assertEquals(1, studentRepository.count());
        }

        /* CREATE WITHOUT TOKEN → 401 */
        @Test
        @DisplayName("Should return 401 when creating student without token")
        void shouldReturn401_WhenNoTokenProvided() throws Exception {
            /* ARRANGE */
            StudentDTO newStudent = buildValidStudent("noaccess@example.com");

            /* ACT AND ASSERT 401 UNAUTHORIZED */
            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newStudent)))
                    .andExpect(status().isUnauthorized());
        }
    }

    /** GET STUDENT BY ID */
    @Nested
    @DisplayName("GET /api/students/{id} - Get Student by ID")
    class GetStudentByIdEndpoint {

        /* GET BY ID WITH JWT → 200 */
        @Test
        @DisplayName("Should return student when ID exists and token is valid")
        void shouldReturnStudent_WhenIdExistsAndTokenIsValid() throws Exception {
            /* ARRANGE - create a student, then retrieve the generated ID */
            String token = getAuthorizationHeader();
            StudentDTO newStudent = buildValidStudent("retrieve.me@example.com");

            MvcResult createResult = mockMvc.perform(post("/api/students")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newStudent)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("id").asLong();

            /* ACT AND ASSERT 200 OK */
            mockMvc.perform(get("/api/students/" + id)
                    .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("retrieve.me@example.com"))
                    .andExpect(jsonPath("$.city").value("Paris"));
        }
    }
}
