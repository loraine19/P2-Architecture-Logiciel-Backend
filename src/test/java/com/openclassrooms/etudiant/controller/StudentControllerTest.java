package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.exception.GlobalExceptionHandler;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UNIT TESTS FOR STUDENT CONTROLLER
 * Uses StandaloneSetup to completely bypass Spring Context and Security issues
 * ./mvnw clean test -Dtest=StudentControllerTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentController Unit Tests")
class StudentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private StudentService studentService;

    @Mock
    private StudentDtoMapper studentMapper;

    @InjectMocks
    private StudentController studentController;

    private StudentDTO testStudentDTO;
    private StudentDTO createdStudentDTO;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        /* INITIALIZE MOCKMVC WITHOUT SPRING CONTEXT + GLOBAL EXCEPTION HANDLER */
        mockMvc = MockMvcBuilders.standaloneSetup(studentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        /* INITIALIZE VALID TEST DATA */
        testStudentDTO = StudentDTO.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .address("1 rue de la Paix")
                .city("Paris")
                .zipCode("75001")
                .phoneNumber("0123456789")
                .build();

        createdStudentDTO = StudentDTO.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .address("1 rue de la Paix")
                .city("Paris")
                .zipCode("75001")
                .phoneNumber("0123456789")
                .build();

        testStudent = Student.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .address("1 rue de la Paix")
                .city("Paris")
                .zipCode("75001")
                .phoneNumber("0123456789")
                .build();
    }

    @Nested
    @DisplayName("GET /api/students - Get All Students")
    class GetAllStudentsEndpoint {

        @Test
        @DisplayName("Should return list of students when service returns data")
        void shouldReturnStudentsList_WhenStudentsExist() throws Exception {
            /* ARRANGE MOCK RESPONSE */
            when(studentService.getAllStudents()).thenReturn(List.of(testStudent));
            when(studentMapper.toDto(any(Student.class))).thenReturn(testStudentDTO);

            /* ACT AND ASSERT 200 OK */
            mockMvc.perform(get("/api/students")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$[0].email").value("alice@example.com"));

            verify(studentService).getAllStudents();
        }

        @Test
        @DisplayName("Should return empty list when no students exist")
        void shouldReturnEmptyList_WhenNoStudentsExist() throws Exception {
            /* ARRANGE EMPTY RESPONSE */
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());

            /* ACT AND ASSERT 200 OK WITH EMPTY ARRAY */
            mockMvc.perform(get("/api/students")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(studentService).getAllStudents();
        }

    }

    @Nested
    @DisplayName("GET /api/students/{id} - Get Student By ID")
    class GetStudentByIdEndpoint {

        @Test
        @DisplayName("Should return student when ID exists")
        void shouldReturnStudent_WhenIdExistsAndAuthenticated() throws Exception {
            /* ARRANGE MOCK RESPONSE */
            when(studentService.getStudentById(1L)).thenReturn(testStudent);
            when(studentMapper.toDto(testStudent)).thenReturn(createdStudentDTO);

            /* ACT AND ASSERT 200 OK */
            mockMvc.perform(get("/api/students/1")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Alice"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("Should return 404 when student ID does not exist")
        void shouldReturn404_WhenStudentNotFound() throws Exception {
            /* ARRANGE EXCEPTION */
            when(studentService.getStudentById(999L))
                    .thenThrow(new EntityNotFoundException("Student not found with ID: 999"));

            /* ACT AND ASSERT 404 NOT FOUND */
            mockMvc.perform(get("/api/students/999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Student not found with ID: 999"));
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400_WhenIdFormatIsInvalid() throws Exception {
            /* ACT AND ASSERT 400 BAD REQUEST */
            mockMvc.perform(get("/api/students/invalid")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    @DisplayName("GET /api/students/email/{email} - Get Student By Email")
    class GetStudentByEmailEndpoint {

        @Test
        @DisplayName("Should return student when email exists")
        void shouldReturnStudent_WhenEmailExists() throws Exception {
            /* ARRANGE */
            when(studentService.getStudentByEmail("alice@example.com")).thenReturn(testStudent);
            when(studentMapper.toDto(testStudent)).thenReturn(createdStudentDTO);

            /* ACT AND ASSERT 200 OK */
            mockMvc.perform(get("/api/students/email/alice@example.com")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("Should return 404 when email does not exist")
        void shouldReturn404_WhenEmailNotFound() throws Exception {
            /* ARRANGE */
            when(studentService.getStudentByEmail("unknown@example.com"))
                    .thenThrow(new EntityNotFoundException("Student not found with email: unknown@example.com"));

            /* ACT AND ASSERT 404 NOT FOUND */
            mockMvc.perform(get("/api/students/email/unknown@example.com")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Student not found with email: unknown@example.com"));
        }

    }

    @Nested
    @DisplayName("POST /api/students - Create Student")
    class CreateStudentEndpoint {

        @Test
        @DisplayName("Should create student when data is valid")
        void shouldCreateStudent_WhenDataIsValidAndAuthenticated() throws Exception {
            /* ARRANGE MOCK RESPONSE */
            when(studentMapper.toEntity(any(StudentDTO.class))).thenReturn(testStudent);
            when(studentService.createStudent(any(Student.class))).thenReturn(testStudent);
            when(studentMapper.toDto(any(Student.class))).thenReturn(createdStudentDTO);

            /* ACT AND ASSERT 201 CREATED */
            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testStudentDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Alice"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"));

            verify(studentService).createStudent(any(Student.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400_WhenValidationFails() throws Exception {
            /* ARRANGE INVALID DTO - missing required fields and invalid email */
            StudentDTO invalidStudentDTO = StudentDTO.builder()
                    .firstName("A")
                    .email("not-valid-email")
                    .build();

            /* ACT AND ASSERT 400 BAD REQUEST */
            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStudentDTO)))
                    .andExpect(status().isBadRequest());

            verify(studentService, never()).createStudent(any());
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400_WhenEmailAlreadyExists() throws Exception {
            /* ARRANGE DUPLICATE EMAIL EXCEPTION */
            when(studentMapper.toEntity(any(StudentDTO.class))).thenReturn(testStudent);
            when(studentService.createStudent(any(Student.class)))
                    .thenThrow(new IllegalArgumentException(
                            "Another student already exists with email: alice@example.com"));

            /* ACT AND ASSERT 400 BAD REQUEST (IllegalArgumentException → 400) */
            mockMvc.perform(post("/api/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testStudentDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
        }

    }

    @Nested
    @DisplayName("PUT /api/students/{id} - Update Student")
    class UpdateStudentEndpoint {

        @Test
        @DisplayName("Should update student when data is valid and ID exists")
        void shouldUpdateStudent_WhenDataIsValidAndIdExists() throws Exception {
            /* ARRANGE MOCK RESPONSE */
            when(studentMapper.toEntity(any(StudentDTO.class))).thenReturn(testStudent);
            when(studentService.updateStudent(anyLong(), any(Student.class))).thenReturn(testStudent);
            when(studentMapper.toDto(any(Student.class))).thenReturn(createdStudentDTO);

            /* ACT AND ASSERT 200 OK */
            mockMvc.perform(put("/api/students/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testStudentDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Alice"));
        }

        @Test
        @DisplayName("Should return 404 when student ID does not exist")
        void shouldReturn404_WhenStudentNotFound() throws Exception {
            /* ARRANGE - entity has no id so controller's mismatch check is skipped */
            Student studentNoId = Student.builder().firstName("Alice").lastName("Smith")
                    .email("alice@example.com").address("1 rue de la Paix")
                    .city("Paris").zipCode("75001").build();
            when(studentMapper.toEntity(any(StudentDTO.class))).thenReturn(studentNoId);
            when(studentService.updateStudent(anyLong(), any(Student.class)))
                    .thenThrow(new EntityNotFoundException("Student not found with ID: 999"));

            /* ACT AND ASSERT 404 NOT FOUND */
            mockMvc.perform(put("/api/students/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testStudentDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Student not found with ID: 999"));
        }

        @Test
        @DisplayName("Should return 400 when path ID and body ID mismatch")
        void shouldReturn400_WhenIdMismatch() throws Exception {
            /* ARRANGE DTO WITH ID = 2 VS PATH ID = 1 */
            StudentDTO mismatchedDTO = StudentDTO.builder()
                    .id(2L)
                    .firstName("Alice")
                    .lastName("Smith")
                    .email("alice@example.com")
                    .address("1 rue de la Paix")
                    .city("Paris")
                    .zipCode("75001")
                    .build();
            Student studentWithId2 = Student.builder().id(2L).firstName("Alice").lastName("Smith")
                    .email("alice@example.com").address("1 rue de la Paix")
                    .city("Paris").zipCode("75001").build();
            when(studentMapper.toEntity(any(StudentDTO.class))).thenReturn(studentWithId2);

            /* ACT AND ASSERT 400 BAD REQUEST */
            mockMvc.perform(put("/api/students/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mismatchedDTO)))
                    .andExpect(status().isBadRequest());

            verify(studentService, never()).updateStudent(anyLong(), any());
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400_WhenValidationFails() throws Exception {
            /* ARRANGE INVALID DTO - empty firstName and invalid email */
            StudentDTO invalidStudentDTO = StudentDTO.builder()
                    .firstName("")
                    .email("not-an-email")
                    .build();

            /* ACT AND ASSERT 400 BAD REQUEST */
            mockMvc.perform(put("/api/students/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStudentDTO)))
                    .andExpect(status().isBadRequest());

            verify(studentService, never()).updateStudent(anyLong(), any());
        }

    }

    @Nested
    @DisplayName("DELETE /api/students/{id} - Delete Student")
    class DeleteStudentEndpoint {

        @Test
        @DisplayName("Should delete student when ID exists")
        void shouldDeleteStudent_WhenIdExistsAndAuthenticated() throws Exception {
            /* ARRANGE MOCK VOID METHOD */
            doNothing().when(studentService).deleteStudent(1L);

            /* ACT AND ASSERT 204 NO CONTENT */
            mockMvc.perform(delete("/api/students/1"))
                    .andExpect(status().isNoContent());

            verify(studentService).deleteStudent(1L);
        }

        @Test
        @DisplayName("Should return 404 when student ID does not exist")
        void shouldReturn404_WhenStudentNotFound() throws Exception {
            /* ARRANGE EXCEPTION */
            doThrow(new EntityNotFoundException("Cannot delete: Student not found with ID: 999"))
                    .when(studentService).deleteStudent(999L);

            /* ACT AND ASSERT 404 NOT FOUND */
            mockMvc.perform(delete("/api/students/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("Cannot delete: Student not found with ID: 999"));
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400_WhenIdFormatIsInvalid() throws Exception {
            /* ACT AND ASSERT 400 BAD REQUEST */
            mockMvc.perform(delete("/api/students/invalid"))
                    .andExpect(status().isBadRequest());
        }

    }

}
