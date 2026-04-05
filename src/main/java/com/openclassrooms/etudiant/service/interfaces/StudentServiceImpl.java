package com.openclassrooms.etudiant.service.interfaces;

import com.openclassrooms.etudiant.entities.Student;

import java.util.List;

/**
 * Service interface for student management operations
 * Defines contracts for CRUD operations on student entities
 */
public interface StudentServiceImpl {

    List<Student> getAllStudents();

    Student getStudentById(Long id);

    Student getStudentByEmail(String email);

    Student createStudent(Student student);

    Student updateStudent(Long id, Student studentDetails);

    void deleteStudent(Long id);
}