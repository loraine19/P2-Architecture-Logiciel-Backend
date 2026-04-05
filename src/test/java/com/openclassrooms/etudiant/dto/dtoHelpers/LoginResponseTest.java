package com.openclassrooms.etudiant.dto.dtoHelpers;

import com.openclassrooms.etudiant.dto.UserProfileDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNIT TESTS FOR LOGIN RESPONSE
 * Tests all three static factory methods: success (3-arg), success (4-arg),
 * error
 */
@DisplayName("LoginResponse Unit Tests")
class LoginResponseTest {

    /** SUCCESS - 3-ARG */
    @Nested
    @DisplayName("success(message, user, authType) - 3-arg factory method")
    class Success3ArgTests {

        @Test
        @DisplayName("Should create a successful response without refresh token")
        void shouldCreateSuccessResponse_WithNoRefreshToken() {
            /* ARRANGE */
            UserProfileDTO profile = new UserProfileDTO();

            /* ACT */
            LoginResponse response = LoginResponse.success("Logged in", profile, AuthType.HEADER);

            /* ASSERT */
            assertEquals("Logged in", response.getMessage());
            assertTrue(response.isSuccess());
            assertEquals(profile, response.getUser());
            assertEquals(AuthType.HEADER, response.getAuthType());
            assertNull(response.getRefreshToken());
        }
    }

    /** SUCCESS - 4-ARG */
    @Nested
    @DisplayName("success(message, user, authType, refreshToken) - 4-arg factory method")
    class Success4ArgTests {

        @Test
        @DisplayName("Should create a successful response with refresh token")
        void shouldCreateSuccessResponse_WithRefreshToken() {
            /* ARRANGE */
            UserProfileDTO profile = new UserProfileDTO();

            /* ACT */
            LoginResponse response = LoginResponse.success(
                    "Logged in", profile, AuthType.HEADER, "some.refresh.token");

            /* ASSERT */
            assertEquals("Logged in", response.getMessage());
            assertTrue(response.isSuccess());
            assertEquals("some.refresh.token", response.getRefreshToken());
        }

        @Test
        @DisplayName("Should create a successful response with null refresh token")
        void shouldCreateSuccessResponse_WithNullRefreshToken() {
            /* ARRANGE */
            UserProfileDTO profile = new UserProfileDTO();

            /* ACT */
            LoginResponse response = LoginResponse.success(
                    "Logged in", profile, AuthType.COOKIE, null);

            /* ASSERT */
            assertTrue(response.isSuccess());
            assertNull(response.getRefreshToken());
        }
    }

    /** ERROR */
    @Nested
    @DisplayName("error(message) - error factory method")
    class ErrorTests {

        @Test
        @DisplayName("Should create an error response with success=false and null user/token")
        void shouldCreateErrorResponse() {
            /* ACT */
            LoginResponse response = LoginResponse.error("Login failed");

            /* ASSERT */
            assertEquals("Login failed", response.getMessage());
            assertFalse(response.isSuccess());
            assertNull(response.getUser());
            assertNull(response.getRefreshToken());
            assertNull(response.getAuthType());
        }
    }
}
