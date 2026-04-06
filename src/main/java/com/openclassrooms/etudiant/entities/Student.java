package com.openclassrooms.etudiant.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Student entity for student management
 * Represents individual students with personal and contact information
 * Includes audit timestamps for data tracking
 */
@Entity
@Table(name = "student")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    @Column(name = "firstName", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    @Column(name = "lastName", nullable = false)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // optional field
    @Pattern(regexp = "^[+]?[0-9\\s\\-().]{10,20}$", message = "Phone number should be valid (10-20 digits with optional formatting)")
    @Column(name = "phoneNumber")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 200, message = "Address must not exceed 200 characters")
    @Column(name = "address")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    @Column(name = "city")
    private String city;

    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "Zip code must be exactly 5 digits")
    @Column(name = "zipCode")
    private String zipCode;

    // camelCase column names match front-end conventions
    @CreationTimestamp
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
}