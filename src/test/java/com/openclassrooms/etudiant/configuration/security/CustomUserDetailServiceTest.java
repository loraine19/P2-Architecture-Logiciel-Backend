package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS FOR CUSTOM USER DETAIL SERVICE
 * Tests the loadUserByUsername method covering all code paths:
 * null login, empty login, whitespace login, not found, valid found
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailService Unit Tests")
class CustomUserDetailServiceTest {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private CustomUserDetailService customUserDetailService;

        private User testUser;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .login("jean.dupont@example.com")
                                .password("encodedPassword")
                                .build();
        }

        /** LOAD USER BY USERNAME */
        @Nested
        @DisplayName("loadUserByUsername - User Loading by Login")
        class LoadUserByUsernameTests {

                /* SUCCESS PATH */
                @Test
                @DisplayName("Should return UserDetails when login is valid and user exists")
                void shouldReturnUserDetails_WhenLoginIsValid() {
                        /* ARRANGE */
                        when(userRepository.findByLogin("jean.dupont@example.com"))
                                        .thenReturn(Optional.of(testUser));

                        /* ACT */
                        UserDetails result = customUserDetailService
                                        .loadUserByUsername("jean.dupont@example.com");

                        /* ASSERT */
                        // not nul
                        assertNotNull(result);
                        // username matches login
                        assertEquals("jean.dupont@example.com", result.getUsername());
                        // method called with correct login
                        verify(userRepository).findByLogin("jean.dupont@example.com");
                }

                /* TRIM PATH */
                @Test
                @DisplayName("Should trim whitespace from login before querying database")
                void shouldTrimLogin_BeforeQueryingDatabase() {
                        /* ARRANGE */
                        when(userRepository.findByLogin("jean.dupont@example.com"))
                                        .thenReturn(Optional.of(testUser));

                        /* ACT - Pass login with surrounding spaces */
                        UserDetails result = customUserDetailService
                                        .loadUserByUsername("  jean.dupont@example.com  ");

                        /* ASSERT - trimmed value is passed to repository */
                        assertNotNull(result);
                        verify(userRepository).findByLogin("jean.dupont@example.com");
                }

                /* NULL LOGIN */
                @Test
                @DisplayName("Should throw UsernameNotFoundException when login is null")
                void shouldThrowUsernameNotFoundException_WhenLoginIsNull() {
                        /* ACT & ASSERT */
                        assertThrows(UsernameNotFoundException.class,
                                        () -> customUserDetailService.loadUserByUsername(null));

                        /* VERIFY - database must not be queried for null login */
                        verifyNoInteractions(userRepository);
                }

                /* EMPTY LOGIN */
                @Test
                @DisplayName("Should throw UsernameNotFoundException when login is empty string")
                void shouldThrowUsernameNotFoundException_WhenLoginIsEmpty() {
                        /* ACT & ASSERT */
                        assertThrows(UsernameNotFoundException.class,
                                        () -> customUserDetailService.loadUserByUsername(""));

                        /* VERIFY - database must not be queried for empty login */
                        verifyNoInteractions(userRepository);
                }

                /* BLANK (WHITESPACE-ONLY) LOGIN */
                @Test
                @DisplayName("Should throw UsernameNotFoundException when login is whitespace only")
                void shouldThrowUsernameNotFoundException_WhenLoginIsBlank() {
                        /* ACT & ASSERT - whitespace gets trimmed to empty, guard fires */
                        assertThrows(UsernameNotFoundException.class,
                                        () -> customUserDetailService.loadUserByUsername("   "));

                        /* VERIFY - database must not be queried for blank login */
                        verifyNoInteractions(userRepository);
                }

                /* USER NOT FOUND IN DB */
                @Test
                @DisplayName("Should throw UsernameNotFoundException when user is not found in database")
                void shouldThrowUsernameNotFoundException_WhenUserNotFound() {
                        /* ARRANGE */
                        when(userRepository.findByLogin("unknown@example.com"))
                                        .thenReturn(Optional.empty());

                        /* ACT & ASSERT */
                        assertThrows(UsernameNotFoundException.class,
                                        () -> customUserDetailService.loadUserByUsername("unknown@example.com"));

                        verify(userRepository).findByLogin("unknown@example.com");
                }
        }
}
