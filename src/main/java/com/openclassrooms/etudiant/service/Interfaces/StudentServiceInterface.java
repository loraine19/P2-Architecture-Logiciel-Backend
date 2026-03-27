package com.openclassrooms.etudiant.service.Interfaces;

import java.util.List;

import com.openclassrooms.etudiant.entities.Student;

public interface StudentServiceInterface {

    List<Student> getAllStudents();

    Student getStudentById(Long id);

    Student getStudentByEmail(String email);

    Student createStudent(Student student);

    Student updateStudent(Long id, Student studentDetails);

    void deleteStudent(Long id);

}