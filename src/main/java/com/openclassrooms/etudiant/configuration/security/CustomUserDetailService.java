package com.openclassrooms.etudiant.configuration.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.openclassrooms.etudiant.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String cleanLogin = (login == null) ? "" : login.trim();

        // avoid DB query on empty input
        if (cleanLogin.isEmpty()) {
            throw new UsernameNotFoundException("Login is empty");
        }

        return userRepository.findByLogin(cleanLogin)
                .orElseThrow(() -> {
                    log.warn("Unknown user: {}", cleanLogin);
                    return new UsernameNotFoundException("Invalid credentials");
                });
    }
}