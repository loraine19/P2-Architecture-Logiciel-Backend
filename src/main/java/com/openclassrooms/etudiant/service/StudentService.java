package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.service.interfaces.StudentServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Service implementation for student management operations
 * Handles CRUD operations with proper validation and error handling
 * Provides logging and transaction management
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentService implements StudentServiceInterface {

    private final StudentRepository studentRepository;

    /**
     * Get all students from database with logging
     */
    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        log.debug("Fetching all students from database");
        List<Student> students = studentRepository.findAll();
        log.info("Found {} students in database", students.size());
        return students;
    }

    /**
     * Find student by ID with proper exception handling
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Student getStudentById(Long id) {
        Assert.notNull(id, "Student ID cannot be null");
        log.debug("Searching for student with ID: {}", id);

        return studentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("Student not found with ID: " + id);
                });
    }

    /**
     * Find student by email with proper exception handling
     */
    @Override
    @Transactional(readOnly = true)
    public Student getStudentByEmail(String email) {
        Assert.hasText(email, "Email cannot be empty");
        log.debug("Searching for student with email: {}", email);

        return studentRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Student not found with email: {}", email);
                    return new EntityNotFoundException("Student not found with email: " + email);
                });
    }

    /**
     * Create new student with duplicate validation
     */
    @Override
    public Student createStudent(Student student) {
        Assert.notNull(student, "Student cannot be null");
        Assert.hasText(student.getEmail(), "Student email cannot be empty");

        log.debug("Creating new student with email: {}", student.getEmail());

        // Check for duplicate email
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            log.warn("Student creation failed - email already exists: {}", student.getEmail());
            throw new IllegalArgumentException("Student already exists with email: " + student.getEmail());
        }

        try {
            Student savedStudent = studentRepository.save(student);
            log.info("Student created successfully: {} {} (ID: {})",
                    savedStudent.getFirstName(), savedStudent.getLastName(), savedStudent.getId());
            return savedStudent;

        } catch (DataIntegrityViolationException e) {
            log.error("Database constraint violation during student creation for email: {}", student.getEmail(), e);
            throw new IllegalArgumentException("Student creation failed due to data constraint violation");
        }
    }

    /**
     * Update existing student with validation
     */
    @Override
    public Student updateStudent(Long id, Student studentDetails) {
        Assert.notNull(id, "Student ID cannot be null");
        Assert.notNull(studentDetails, "Student details cannot be null");

        log.debug("Updating student with ID: {}", id);

        Student existingStudent = getStudentById(id);

        // Check for email conflicts (excluding current student)
        if (studentDetails.getEmail() != null &&
                !studentDetails.getEmail().equals(existingStudent.getEmail())) {
            if (studentRepository.findByEmail(studentDetails.getEmail()).isPresent()) {
                log.warn("Student update failed - email already exists: {}", studentDetails.getEmail());
                throw new IllegalArgumentException(
                        "Another student already exists with email: " + studentDetails.getEmail());
            }
        }

        // Update fields
        existingStudent.setFirstName(studentDetails.getFirstName());
        existingStudent.setLastName(studentDetails.getLastName());
        existingStudent.setEmail(studentDetails.getEmail());
        existingStudent.setPhoneNumber(studentDetails.getPhoneNumber());
        existingStudent.setAddress(studentDetails.getAddress());
        existingStudent.setCity(studentDetails.getCity());
        existingStudent.setZipCode(studentDetails.getZipCode());

        try {
            Student updatedStudent = studentRepository.save(existingStudent);
            log.info("Student updated successfully: {} {} (ID: {})",
                    updatedStudent.getFirstName(), updatedStudent.getLastName(), updatedStudent.getId());
            return updatedStudent;

        } catch (DataIntegrityViolationException e) {
            log.error("Database constraint violation during student update for ID: {}", id, e);
            throw new IllegalArgumentException("Student update failed due to data constraint violation");
        }
    }

    /**
     * Delete student with existence validation
     */
    @Override
    @SuppressWarnings("null")
    public void deleteStudent(Long id) {
        Assert.notNull(id, "Student ID cannot be null");
        log.debug("Attempting to delete student with ID: {}", id);

        if (!studentRepository.existsById(id)) {
            log.warn("Student deletion failed - not found with ID: {}", id);
            throw new EntityNotFoundException("Cannot delete: Student not found with ID: " + id);
        }

        studentRepository.deleteById(id);
        log.info("Student deleted successfully with ID: {}", id);
    }
}