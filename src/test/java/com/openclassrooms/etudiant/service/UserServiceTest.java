package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.configuration.security.CustomUserDetailService;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.UserDTO;
import com.openclassrooms.etudiant.dto.UserProfileDTO;
import com.openclassrooms.etudiant.dto.dtoHelpers.AuthType;
import com.openclassrooms.etudiant.dto.dtoHelpers.LoginResponse;
import com.openclassrooms.etudiant.dto.dtoHelpers.MessageResp;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS FOR USER SERVICE
 * Tests business logic for authentication, registration and logout
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtService jwtService;

        @Mock
        private UserDtoMapper userDtoMapper;

        @Mock
        private CustomUserDetailService userDetailsService;

        @Mock
        private HttpServletRequest httpServletRequest;

        @Mock
        private HttpServletResponse httpServletResponse;

        @InjectMocks
        private UserService userService;

        private UserDTO testUserDTO;
        private User testUser;
        private User savedUser;
        private LoginRequestDTO loginRequestDTO;

        @BeforeEach
        void setUp() {
                /* INITIALIZE TEST DATA */
                testUserDTO = UserDTO.builder()
                                .firstName("Jean")
                                .lastName("Dupont")
                                .login("jean.dupont@example.com")
                                .password("Password123!")
                                .build();

                testUser = User.builder()
                                .id(1L)
                                .firstName("Jean")
                                .lastName("Dupont")
                                .login("jean.dupont@example.com")
                                .password("encodedPassword")
                                .build();

                savedUser = testUser;

                loginRequestDTO = new LoginRequestDTO();
                loginRequestDTO.setLogin("jean.dupont@example.com");
                loginRequestDTO.setPassword("Password123!");
                loginRequestDTO.setAuthType(AuthType.HEADER);
        }

        @Nested
        @DisplayName("User Registration Logic")
        class UserRegistrationTests {

                /* REGISTER SUCCESS */
                @Test
                @DisplayName("Should register user successfully with valid data")
                void shouldRegisterUser_WhenDataIsValid() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(testUserDTO.getLogin())).thenReturn(Optional.empty());
                        when(userDtoMapper.toEntity(testUserDTO)).thenReturn(testUser);
                        when(passwordEncoder.encode(testUserDTO.getPassword())).thenReturn("encodedPassword");
                        when(userRepository.save(any(User.class))).thenReturn(savedUser);

                        /* ACT */
                        ResponseEntity<MessageResp> response = userService.register(testUserDTO);

                        /* ASSERT */
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertTrue(response.getBody().getMessage().contains("successfully"));
                        verify(userRepository, times(1)).save(any(User.class));
                }

                /* REGISTER NULL DTO */
                @Test
                @DisplayName("Should throw IllegalArgumentException when UserDTO is null")
                void shouldThrowIllegalArgumentException_WhenUserDTOIsNull() {
                        /* ACT & ASSERT */
                        assertThrows(IllegalArgumentException.class, () -> userService.register(null));
                }

                /* REGISTER DUPLICATE LOGIN */
                @Test
                @DisplayName("Should return error when user already exists")
                void shouldReturnError_WhenUserAlreadyExists() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(testUserDTO.getLogin())).thenReturn(Optional.of(testUser));

                        /* ACT */
                        ResponseEntity<MessageResp> response = userService.register(testUserDTO);

                        /* ASSERT */
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals("Login already exists", response.getBody().getMessage());
                }
        }

        @Nested
        @DisplayName("User Authentication Logic")
        class UserAuthenticationTests {

                /* LOGIN SUCCESS */
                @Test
                @DisplayName("Should authenticate user successfully with valid credentials")
                void shouldAuthenticateUser_WhenCredentialsAreValid() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(loginRequestDTO.getLogin())).thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword()))
                                        .thenReturn(true);
                        when(jwtService.generateToken(testUser, false)).thenReturn("mock.jwt.token");
                        when(userDtoMapper.toProfileDto(testUser)).thenReturn(new UserProfileDTO());

                        /* ACT */
                        ResponseEntity<LoginResponse> response = userService.login(loginRequestDTO, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertTrue(response.getBody().isSuccess());
                        verify(jwtService).generateToken(testUser, false);
                }

                /* LOGIN INVALID CREDENTIALS */
                @Test
                @DisplayName("Should return 401 when user not found")
                void shouldReturn401_WhenUserNotFound() {
                        /* ARRANGE */
                        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

                        /* ACT */
                        ResponseEntity<LoginResponse> response = userService.login(loginRequestDTO, httpServletRequest,
                                        httpServletResponse);

                        /* ASSERT */
                        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                }
        }

        @Nested
        @DisplayName("User Logout Logic")
        class UserLogoutTests {

                /* LOGOUT SUCCESS */
                @Test
                @DisplayName("Should logout user successfully")
                void shouldLogoutUser_Successfully() {
                        /* ACT */
                        ResponseEntity<MessageResp> response = userService.logout(httpServletResponse);

                        /* ASSERT */
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertEquals("User logged out successfully", response.getBody().getMessage());

                        /* VERIFY TWO COOKIES DELETIONS (token and refreshToken) */
                        verify(httpServletResponse, times(2)).addHeader(eq("Set-Cookie"), anyString());
                }
        }
}