package com.openclassrooms.etudiant.repository;

import com.openclassrooms.etudiant.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Student entity database operations
 * Provides CRUD operations and custom queries for student management
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    /**
     * Find student by unique email address
     * Used for duplicate email validation and student lookup
     */
    Optional<Student> findByEmail(String email);
}
