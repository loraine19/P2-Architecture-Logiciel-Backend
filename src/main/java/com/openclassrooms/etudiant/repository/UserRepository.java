package com.openclassrooms.etudiant.repository;

import com.openclassrooms.etudiant.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity database operations
 * Provides CRUD operations and authentication-related queries
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by unique login identifier
     * Used for authentication and user lookup
     */
    Optional<User> findByLogin(String login);
}
