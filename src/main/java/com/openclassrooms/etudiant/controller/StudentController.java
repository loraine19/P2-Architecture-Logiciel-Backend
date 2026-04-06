package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for student management
 * Handles CRUD operations for students
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final StudentDtoMapper studentMapper;

    /* GET ALL STUDENTS */
    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students = studentService.getAllStudents()
                .stream()
                .map(studentMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Found {} students", students.size());
        return ResponseEntity.ok(students);
    }

    /* GET BY ID */
    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        return ResponseEntity.ok(studentMapper.toDto(student));
    }

    /* GET BY EMAIL */
    @GetMapping("/email/{email}")
    public ResponseEntity<StudentDTO> getStudentByEmail(@PathVariable String email) {
        Student student = studentService.getStudentByEmail(email);
        return ResponseEntity.ok(studentMapper.toDto(student));
    }

    /* CREATE STUDENT */
    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        Student studentRequest = studentMapper.toEntity(studentDTO);
        Student createdStudent = studentService.createStudent(studentRequest);
        log.debug("Student created with id: {}", createdStudent.getId());
        return new ResponseEntity<>(studentMapper.toDto(createdStudent), HttpStatus.CREATED);
    }

    /* UPDATE STUDENT */
    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDTO) {
        Student studentRequest = studentMapper.toEntity(studentDTO);

        // reject if path and body IDs differ — prevents accidental overwrites
        if (studentRequest.getId() != null && !studentRequest.getId().equals(id)) {
            log.warn("ID mismatch: path={}, body={}", id, studentRequest.getId());
            return ResponseEntity.badRequest().build();
        }

        Student updatedStudent = studentService.updateStudent(id, studentRequest);
        return ResponseEntity.ok(studentMapper.toDto(updatedStudent));
    }

    /* DELETE STUDENT */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(
            @Valid @PathVariable Long id) {
        log.debug("Deleting student with id: {}", id);
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}