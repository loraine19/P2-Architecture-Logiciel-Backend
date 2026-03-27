package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.service.Interfaces.StudentServiceInterface;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService implements StudentServiceInterface {

    private final StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        System.out.println(students);
        return students;
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElse(null);
    }

    @Transactional
    public Student createStudent(Student student) {
        Student existingStudent = getStudentByEmail(student.getEmail());
        if (existingStudent != null) {
            throw new RuntimeException("Student already exists with email: " + student.getEmail());
        }
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName(student.getFirstName());
        studentDTO.setLastName(student.getLastName());
        studentDTO.setEmail(student.getEmail());
        studentDTO.setPhoneNumber(student.getPhoneNumber());
        studentDTO.setAddress(student.getAddress());
        studentDTO.setCity(student.getCity());
        studentDTO.setZipCode(student.getZipCode());
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student studentDetails) {
        Student student = getStudentById(id);

        student.setFirstName(studentDetails.getFirstName());
        student.setLastName(studentDetails.getLastName());
        student.setEmail(studentDetails.getEmail());
        student.setPhoneNumber(studentDetails.getPhoneNumber());
        student.setAddress(studentDetails.getAddress());
        student.setCity(studentDetails.getCity());
        student.setZipCode(studentDetails.getZipCode());

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete: Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }
}