package com.openclassrooms.etudiant.dto.dtoHelpers;

import com.openclassrooms.etudiant.dto.UserDTO;
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

    /** USER DTO BUILDER */
    @Nested
    @DisplayName("UserDTO.UserDTOBuilder - Builder pattern")
    class UserDTOBuilderTests {

        /* ALL FIELDS */
        @Test
        @DisplayName("Should build UserDTO with all fields set")
        void shouldBuildUserDTO_WithAllFields() {
            /* ACT */
            UserDTO dto = UserDTO.builder()
                    .id(1L)
                    .firstName("Jean")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            /* ASSERT */
            assertEquals(1L, dto.getId());
            assertEquals("Jean", dto.getFirstName());
            assertEquals("Dupont", dto.getLastName());
            assertEquals("jean.dupont@example.com", dto.getLogin());
            assertEquals("P@ssw0rd!", dto.getPassword());
        }

        /* NO ID (optional field) */
        @Test
        @DisplayName("Should build UserDTO with null id when id is not set")
        void shouldBuildUserDTO_WithNullIdByDefault() {
            /* ACT */
            UserDTO dto = UserDTO.builder()
                    .firstName("Jean")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            /* ASSERT */
            assertNull(dto.getId());
        }

        /* EQUALITY */
        @Test
        @DisplayName("Should be equal to another UserDTO built with identical values")
        void shouldBeEqual_WhenBuiltWithSameValues() {
            /* ARRANGE */
            UserDTO dto1 = UserDTO.builder()
                    .id(1L)
                    .firstName("Jean")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            UserDTO dto2 = UserDTO.builder()
                    .id(1L)
                    .firstName("Jean")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            /* ASSERT */
            assertEquals(dto1, dto2);
            assertEquals(dto1.hashCode(), dto2.hashCode());
        }

        /* NOT EQUAL WHEN DIFFERENT VALUES */
        @Test
        @DisplayName("Should not be equal when any field differs")
        void shouldNotBeEqual_WhenFieldsDiffer() {
            /* ARRANGE */
            UserDTO dto1 = UserDTO.builder()
                    .firstName("Jean")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            UserDTO dto2 = UserDTO.builder()
                    .firstName("Marie")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            /* ASSERT */
            assertNotEquals(dto1, dto2);
        }

        /* SETTERS (from @Data) */
        @Test
        @DisplayName("Should update fields via setters generated by @Data")
        void shouldUpdateFields_ViaSetters() {
            /* ARRANGE */
            UserDTO dto = UserDTO.builder()
                    .firstName("Jean")
                    .lastName("Dupont")
                    .login("jean.dupont@example.com")
                    .password("P@ssw0rd!")
                    .build();

            /* ACT */
            dto.setFirstName("Pierre");
            dto.setLogin("pierre.dupont@example.com");

            /* ASSERT */
            assertEquals("Pierre", dto.getFirstName());
            assertEquals("pierre.dupont@example.com", dto.getLogin());
        }

        /* NO-ARGS CONSTRUCTOR */
        @Test
        @DisplayName("Should create empty UserDTO via no-args constructor with all null fields")
        void shouldCreateEmptyUserDTO_ViaNoArgsConstructor() {
            /* ACT */
            UserDTO dto = new UserDTO();

            /* ASSERT */
            assertNull(dto.getId());
            assertNull(dto.getFirstName());
            assertNull(dto.getLastName());
            assertNull(dto.getLogin());
            assertNull(dto.getPassword());
        }

        /* ALL-ARGS CONSTRUCTOR */
        @Test
        @DisplayName("Should create UserDTO via all-args constructor")
        void shouldCreateUserDTO_ViaAllArgsConstructor() {
            /* ACT */
            UserDTO dto = new UserDTO(2L, "Marie", "Martin", "marie.martin@example.com", "P@ssw0rd!");

            /* ASSERT */
            assertEquals(2L, dto.getId());
            assertEquals("Marie", dto.getFirstName());
            assertEquals("Martin", dto.getLastName());
            assertEquals("marie.martin@example.com", dto.getLogin());
            assertEquals("P@ssw0rd!", dto.getPassword());
        }

        /* EQUALS - NULL FIELD vs NON-NULL */
        @Test
        @DisplayName("Should not be equal when one side has null field and other has value")
        void shouldNotBeEqual_WhenOneFieldIsNullAndOtherIsNot() {
            /* ARRANGE - dto1 has null firstName, dto2 has a value */
            UserDTO dto1 = UserDTO.builder()
                    .login("a@example.com")
                    .build();
            UserDTO dto2 = UserDTO.builder()
                    .firstName("Jean")
                    .login("a@example.com")
                    .build();

            /* ASSERT - covers the null != not-null branch in generated equals() */
            assertNotEquals(dto1, dto2);
        }

        /* EQUALS - BOTH FIELDS NULL */
        @Test
        @DisplayName("Should be equal when both DTOs have only null fields in common")
        void shouldBeEqual_WhenBothDTOsHaveSameNullFields() {
            /* ARRANGE - only login set, all other fields null */
            UserDTO dto1 = UserDTO.builder().login("a@example.com").build();
            UserDTO dto2 = UserDTO.builder().login("a@example.com").build();

            /* ASSERT - covers the null == null branch in generated equals() */
            assertEquals(dto1, dto2);
        }

        /* EQUALS - SAME INSTANCE */
        @Test
        @DisplayName("Should be equal to itself (identity check)")
        void shouldBeEqual_ToSameInstance() {
            /* ARRANGE */
            UserDTO dto = UserDTO.builder().firstName("Jean").login("a@example.com").build();

            /* ASSERT - covers the this == other fast-path in generated equals() */
            assertEquals(dto, dto);
        }

        /* EQUALS - NULL */
        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqual_ToNull() {
            /* ARRANGE */
            UserDTO dto = UserDTO.builder().firstName("Jean").build();

            /* ASSERT - covers the instanceof null check in generated equals() */
            assertNotEquals(null, dto);
        }

        /* EQUALS - DIFFERENT TYPE */
        @Test
        @DisplayName("Should not be equal to an object of a different type")
        void shouldNotBeEqual_ToDifferentType() {
            /* ARRANGE */
            UserDTO dto = UserDTO.builder().firstName("Jean").build();

            /* ASSERT - covers the !instanceof branch in generated equals() */
            assertNotEquals("not a UserDTO", dto);
        }

        /* TOSTRING ON BUILDER */
        @Test
        @DisplayName("Should produce non-blank toString from the builder before build()")
        void shouldProduceNonBlankToString_FromBuilder() {
            /* ARRANGE */
            UserDTO.UserDTOBuilder builder = UserDTO.builder()
                    .firstName("Jean")
                    .login("jean@example.com");

            /* ACT */
            String result = builder.toString();

            /* ASSERT - covers Lombok-generated toString() on the builder itself */
            assertNotNull(result);
            assertFalse(result.isBlank());
            assertTrue(result.contains("Jean"));
        }
    }
}
