package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.service.interfaces.StudentServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class StudentService implements StudentServiceImpl {

    private final StudentRepository studentRepository;

    /** PUBLIC STUDENTS METHODS */

    /* GET ALL */
    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        List<Student> students = studentRepository.findAll();

        return students;
    }

    /* GET BY ID */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Student getStudentById(Long id) {
        Assert.notNull(id, "Student ID cannot be null");
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("Student not found with ID: " + id);
                });
        return student;
    }

    /* GET BY EMAIL */
    @Override
    // TODO verfi transactional read only
    public Student getStudentByEmail(String email) {
        Assert.hasText(email, "Email cannot be empty");
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Student not found with email: {}", email);
                    return new EntityNotFoundException("Student not found with email: " + email);
                });
        return student;
    }

    /* CREATE */
    @Override
    public Student createStudent(Student student) {
        Assert.notNull(student, "Student cannot be null");
        Assert.hasText(student.getEmail(), "Student email cannot be empty");

        // Check for duplicate email
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            log.warn("Student creation failed - email already exists: {}", student.getEmail());
            throw new IllegalArgumentException(
                    "Another student already exists with email: " + student.getEmail());
        }

        // Save student
        Student savedStudent = studentRepository.save(student);
        return savedStudent;
    }

    /* UPDATE */
    @Override
    public Student updateStudent(Long id, Student studentDetails) {
        Assert.notNull(id, "Student ID cannot be null");
        Assert.notNull(studentDetails, "Student details cannot be null");

        // Get existing student
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

        // Save updated student
        Student updatedStudent = studentRepository.save(existingStudent);
        return updatedStudent;

    }

    /* DELETE */
    @Override
    public void deleteStudent(Long id) {
        Assert.notNull(id, "Student ID cannot be null");

        if (!studentRepository.existsById(id)) {
            log.warn("Student deletion failed - not found with ID: {}", id);
            throw new EntityNotFoundException("Cannot delete: Student not found with ID: " + id);
        }

        studentRepository.deleteById(id);
    }

}