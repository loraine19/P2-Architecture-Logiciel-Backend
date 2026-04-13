package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.messages.StudentErrorMessage;
import com.openclassrooms.etudiant.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS FOR STUDENT SERVICE
 * Tests all CRUD operations, validation, and error handling
 * ./mvnw clean test -Dtest=StudentServiceTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student testStudent;
    private Student savedStudent;

    @BeforeEach
    void setUp() {
        /* INITIALIZE TEST DATA WITHOUT ID (for creation) */
        testStudent = Student.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .address("1 rue de la Paix")
                .city("Paris")
                .zipCode("75001")
                .phoneNumber("0123456789")
                .build();

        /* SAVED STUDENT WITH ID SET (simulates returned entity from DB) */
        savedStudent = Student.builder()
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
    @DisplayName("Get All Students")
    class GetAllStudentsTests {

        @Test
        @DisplayName("Should return list of students when students exist")
        void shouldReturnStudentsList_WhenStudentsExist() {
            /* ARRANGE */
            when(studentRepository.findAll()).thenReturn(List.of(savedStudent));

            /* ACT */
            List<Student> result = studentService.getAllStudents();

            /* ASSERT */
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("alice@example.com", result.get(0).getEmail());
        }

        @Test
        @DisplayName("Should return empty list when no students exist")
        void shouldReturnEmptyList_WhenNoStudentsExist() {
            /* ARRANGE */
            when(studentRepository.findAll()).thenReturn(Collections.emptyList());

            /* ACT */
            List<Student> result = studentService.getAllStudents();

            /* ASSERT */
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Student By ID")
    class GetStudentByIdTests {

        @Test
        @DisplayName("Should return student when ID exists")
        void shouldReturnStudent_WhenIdExists() {
            /* ARRANGE */
            when(studentRepository.findById(1L)).thenReturn(Optional.of(savedStudent));

            /* ACT */
            Student result = studentService.getStudentById(1L);

            /* ASSERT */
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("alice@example.com", result.getEmail());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when ID does not exist")
        void shouldThrowEntityNotFoundException_WhenIdDoesNotExist() {
            /* ARRANGE */
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            /* ACT & ASSERT */
            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> studentService.getStudentById(999L));
            assertTrue(ex.getMessage().contains("999"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when ID is null")
        void shouldThrowIllegalArgumentException_WhenIdIsNull() {
            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.getStudentById(null));
        }
    }

    @Nested
    @DisplayName("Create Student")
    class CreateStudentTests {

        @Test
        @DisplayName("Should create student successfully with valid data")
        void shouldCreateStudent_WhenDataIsValid() {
            /* ARRANGE */
            when(studentRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

            /* ACT */
            Student result = studentService.createStudent(testStudent);

            /* ASSERT */
            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(studentRepository).save(testStudent);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when student is null")
        void shouldThrowIllegalArgumentException_WhenStudentIsNull() {
            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.createStudent(null));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email already exists")
        void shouldThrowIllegalArgumentException_WhenEmailAlreadyExists() {
            /* ARRANGE */
            when(studentRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedStudent));

            /* ACT & ASSERT */
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> studentService.createStudent(testStudent));
            assertTrue(ex.getMessage().contains("alice@example.com"));
        }

        // : branche Assert.hasText(email) dans
        // createStudent() non couverte
        @Test
        @DisplayName("Should throw IllegalArgumentException when email is blank")
        void shouldThrowIllegalArgumentException_WhenEmailIsBlank() {
            /* ARRANGE */
            Student studentWithBlankEmail = Student.builder()
                    .firstName("Alice").lastName("Smith")
                    .email("").address("1 rue").city("Paris").zipCode("75001").build();

            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.createStudent(studentWithBlankEmail));
        }

        @Test
        @DisplayName("Should throw DataIntegrityViolationException when database constraint violated")
        void shouldThrowDataIntegrityViolationException_WhenConstraintViolated() {
            /* ARRANGE */
            when(studentRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(studentRepository.save(any(Student.class))).thenThrow(DataIntegrityViolationException.class);

            /* ACT & ASSERT */
            assertThrows(DataIntegrityViolationException.class,
                    () -> studentService.createStudent(testStudent));
        }
    }

    @Nested
    @DisplayName("Update Student")
    class UpdateStudentTests {

        @Test
        @DisplayName("Should update student successfully when ID exists")
        void shouldUpdateStudent_WhenIdExists() {
            /* ARRANGE */
            Student updatedDetails = Student.builder()
                    .firstName("Bob").lastName("Jones").email("alice@example.com")
                    .address("2 avenue").city("Lyon").zipCode("69001").build();
            Student updatedResult = Student.builder().id(1L)
                    .firstName("Bob").lastName("Jones").email("alice@example.com")
                    .address("2 avenue").city("Lyon").zipCode("69001").build();
            when(studentRepository.findById(1L)).thenReturn(Optional.of(savedStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(updatedResult);

            /* ACT */
            Student result = studentService.updateStudent(1L, updatedDetails);

            /* ASSERT */
            assertEquals("Bob", result.getFirstName());
            assertEquals("Lyon", result.getCity());
            verify(studentRepository).save(any(Student.class));
        }

        // : branche Assert.notNull(id) dans updateStudent()
        // non couverte
        @Test
        @DisplayName("Should throw IllegalArgumentException when update ID is null")
        void shouldThrowIllegalArgumentException_WhenUpdateIdIsNull() {
            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.updateStudent(null, testStudent));
        }

        // : branche email inchangé (skip findByEmail) dans
        // updateStudent() non couverte
        @Test
        @DisplayName("Should update student and skip email conflict check when email is unchanged")
        void shouldUpdateStudent_WhenEmailIsUnchanged() {
            /* ARRANGE - same email as existing → conflict check branch is skipped */
            Student updatedDetails = Student.builder()
                    .firstName("Alice2").lastName("Smith").email("alice@example.com")
                    .address("2 avenue").city("Lyon").zipCode("69001").build();
            Student updatedResult = Student.builder().id(1L)
                    .firstName("Alice2").lastName("Smith").email("alice@example.com")
                    .address("2 avenue").city("Lyon").zipCode("69001").build();
            when(studentRepository.findById(1L)).thenReturn(Optional.of(savedStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(updatedResult);

            /* ACT */
            Student result = studentService.updateStudent(1L, updatedDetails);

            /* ASSERT - email unchanged means findByEmail must NOT be called */
            assertEquals("Alice2", result.getFirstName());
            verify(studentRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when ID does not exist")
        void shouldThrowEntityNotFoundException_WhenIdDoesNotExist() {
            /* ARRANGE */
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            /* ACT & ASSERT */
            assertThrows(EntityNotFoundException.class,
                    () -> studentService.updateStudent(999L, testStudent));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when student data is null")
        void shouldThrowIllegalArgumentException_WhenStudentIsNull() {
            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.updateStudent(1L, null));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email already exists for different student")
        void shouldThrowIllegalArgumentException_WhenEmailExistsForDifferentStudent() {
            /* ARRANGE - different student with same email already in DB */
            Student otherStudent = Student.builder().id(2L).email("other@example.com").build();
            Student updateWithConflictEmail = Student.builder()
                    .firstName("Alice").lastName("Smith").email("other@example.com")
                    .address("1 rue").city("Paris").zipCode("75001").build();
            when(studentRepository.findById(1L)).thenReturn(Optional.of(savedStudent));
            when(studentRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherStudent));

            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.updateStudent(1L, updateWithConflictEmail));
        }
    }

    @Nested
    @DisplayName("Delete Student")
    class DeleteStudentTests {

        @Test
        @DisplayName("Should delete student successfully when ID exists")
        void shouldDeleteStudent_WhenIdExists() {
            /* ARRANGE */
            when(studentRepository.existsById(1L)).thenReturn(true);
            doNothing().when(studentRepository).deleteById(1L);

            /* ACT */
            studentService.deleteStudent(1L);

            /* ASSERT */
            verify(studentRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when ID does not exist")
        void shouldThrowEntityNotFoundException_WhenIdDoesNotExist() {
            /* ARRANGE */
            when(studentRepository.existsById(999L)).thenReturn(false);

            /* ACT & ASSERT */
            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> studentService.deleteStudent(999L));
            assertTrue(ex.getMessage().contains("999"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when ID is null")
        void shouldThrowIllegalArgumentException_WhenIdIsNull() {
            /* ACT & ASSERT */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.deleteStudent(null));
        }
    }

    @Nested
    @DisplayName("Find Student By Email")
    class FindStudentByEmailTests {

        @Test
        @DisplayName("Should return student when email exists")
        void shouldReturnStudent_WhenEmailExists() {
            /* ARRANGE */
            when(studentRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedStudent));

            /* ACT */
            Student result = studentService.getStudentByEmail("alice@example.com");

            /* ASSERT */
            assertNotNull(result);
            assertEquals("alice@example.com", result.getEmail());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when email does not exist")
        void shouldThrowEntityNotFoundException_WhenEmailDoesNotExist() {
            /* ARRANGE */
            when(studentRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            /* ACT & ASSERT */
            assertThrows(EntityNotFoundException.class,
                    () -> studentService.getStudentByEmail("unknown@example.com"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email is null or empty")
        void shouldThrowIllegalArgumentException_WhenEmailIsNullOrEmpty() {
            /* ACT & ASSERT - null email */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.getStudentByEmail(null));
            /* ACT & ASSERT - empty email */
            assertThrows(IllegalArgumentException.class,
                    () -> studentService.getStudentByEmail(""));
        }
    }

    /** STUDENT ERROR MESSAGE ENUM */
    @Nested
    @DisplayName("StudentErrorMessage - Enum messages and format")
    class StudentErrorMessageTests {

        /* GET MESSAGE */
        @Test
        @DisplayName("Should return non-blank message for every enum value")
        void shouldReturnNonBlankMessage_ForEveryValue() {
            /* ACT & ASSERT */
            for (StudentErrorMessage msg : StudentErrorMessage.values()) {
                assertNotNull(msg.getMessage());
                assertFalse(msg.getMessage().isBlank());
            }
        }

        /* FORMAT NOT_FOUND_ID */
        @Test
        @DisplayName("Should format NOT_FOUND_ID with the student id")
        void shouldFormatNotFoundId_WithId() {
            /* ACT */
            String result = StudentErrorMessage.NOT_FOUND_ID.format(42L);

            /* ASSERT */
            assertTrue(result.contains("42"));
        }

        /* FORMAT NOT_FOUND_EMAIL */
        @Test
        @DisplayName("Should format NOT_FOUND_EMAIL with the email")
        void shouldFormatNotFoundEmail_WithEmail() {
            /* ACT */
            String result = StudentErrorMessage.NOT_FOUND_EMAIL.format("jean@example.com");

            /* ASSERT */
            assertTrue(result.contains("jean@example.com"));
        }

        /* FORMAT EMAIL_EXISTS */
        @Test
        @DisplayName("Should format EMAIL_EXISTS with the email")
        void shouldFormatEmailExists_WithEmail() {
            /* ACT */
            String result = StudentErrorMessage.EMAIL_EXISTS.format("jean@example.com");

            /* ASSERT */
            assertTrue(result.contains("jean@example.com"));
        }

        /* FORMAT DELETE_NOT_FOUND */
        @Test
        @DisplayName("Should format DELETE_NOT_FOUND with the student id")
        void shouldFormatDeleteNotFound_WithId() {
            /* ACT */
            String result = StudentErrorMessage.DELETE_NOT_FOUND.format(99L);

            /* ASSERT */
            assertTrue(result.contains("99"));
        }
    }

    /** STUDENT ERROR MESSAGE ENUM */
    @Nested
    @DisplayName("StudentErrorMessage - Enum messages and format")
    class StudentMessageTests {

        /* GET MESSAGE */
        @Test
        @DisplayName("Should return non-blank message for every enum value")
        void shouldReturnNonBlankMessage_ForEveryValue() {
            /* ACT & ASSERT */
            for (StudentErrorMessage msg : StudentErrorMessage.values()) {
                assertNotNull(msg.getMessage());
                assertFalse(msg.getMessage().isBlank());
            }
        }

        /* FORMAT NOT_FOUND_ID */
        @Test
        @DisplayName("Should format NOT_FOUND_ID with the student id")
        void shouldFormatNotFoundById_WithId() {
            /* ACT */
            String result = StudentErrorMessage.NOT_FOUND_ID.format(7L);

            /* ASSERT */
            assertTrue(result.contains("7"));
        }

        /* FORMAT NOT_FOUND_EMAIL */
        @Test
        @DisplayName("Should format NOT_FOUND_EMAIL with the email")
        void shouldFormatNotFoundByEmail_WithEmail() {
            /* ACT */
            String result = StudentErrorMessage.NOT_FOUND_EMAIL.format("test@example.com");

            /* ASSERT */
            assertTrue(result.contains("test@example.com"));
        }

        /* FORMAT EMAIL_EXISTS */
        @Test
        @DisplayName("Should format EMAIL_EXISTS with the email")
        void shouldFormatEmailAlreadyExists_WithEmail() {
            /* ACT */
            String result = StudentErrorMessage.EMAIL_EXISTS.format("dup@example.com");

            /* ASSERT */
            assertTrue(result.contains("dup@example.com"));
        }
    }
}