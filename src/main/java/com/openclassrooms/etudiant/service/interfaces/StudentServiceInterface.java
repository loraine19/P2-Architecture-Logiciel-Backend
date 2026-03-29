package com.openclassrooms.etudiant.service.interfaces;

import com.openclassrooms.etudiant.entities.Student;

import java.util.List;

/**
 * Service interface for student management operations
 * Defines contracts for CRUD operations on student entities
 */
public interface StudentServiceInterface {

    /**
     * Retrieve all students from the database
     */
    List<Student> getAllStudents();

    /**
     * Find student by unique identifier
     */
    Student getStudentById(Long id);

    /**
     * Find student by unique email address
     */
    Student getStudentByEmail(String email);

    /**
     * Create a new student record
     */
    Student createStudent(Student student);

    /**
     * Update existing student information
     */
    Student updateStudent(Long id, Student studentDetails);

    /**
     * Remove student from the database
     */
    void deleteStudent(Long id);
}