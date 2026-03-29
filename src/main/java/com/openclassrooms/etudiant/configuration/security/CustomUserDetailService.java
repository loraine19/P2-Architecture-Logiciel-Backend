package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom service to load user details for authentication
 * Used by Spring Security during login process
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Check if login is valid
        if (login == null || login.trim().isEmpty()) {
            log.warn("Login attempt with empty username");
            throw new IllegalArgumentException("Login cannot be empty");
        }

        log.debug("Auth attempt for user: {}", login);

        // Find user in database
        return userRepository.findByLogin(login.trim())
                .orElseThrow(() -> {
                    log.warn("Auth failed for user: {}", login);
                    // Generic message for security
                    return new UsernameNotFoundException("Invalid credentials");
                });
    }

}
