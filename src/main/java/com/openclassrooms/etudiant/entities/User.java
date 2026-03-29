package com.openclassrooms.etudiant.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User entity for authentication and authorization
 * Implements UserDetails for Spring Security integration
 * Represents application users with login credentials
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Personal information
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Column(name = "firstName", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Column(name = "lastName", nullable = false)
    private String lastName;

    // Authentication credentials
    @NotBlank(message = "Login is required")
    @Size(max = 100, message = "Login must not exceed 100 characters")
    @Column(name = "login", unique = true, nullable = false)
    private String login;

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must not exceed 255 characters")
    @Column(name = "password", nullable = false)
    private String password;

    // Audit timestamps with consistent naming (camelCase)
    @CreationTimestamp
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    // JwtRefreshToken
    @Column(name = "refreshToken", nullable = true)
    private String refreshToken;

    // Spring Security UserDetails implementation

    /**
     * Return user authorities/roles - currently none (basic auth only)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // No roles implemented yet
    }

    /**
     * Return username for Spring Security (uses login field)
     */
    @Override
    public String getUsername() {
        return login;
    }

    /**
     * Account never expires in this basic implementation
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account never locked in this basic implementation
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credentials never expire in this basic implementation
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Account always enabled in this basic implementation
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
