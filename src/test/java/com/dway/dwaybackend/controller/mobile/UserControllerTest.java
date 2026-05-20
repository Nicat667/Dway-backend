package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.exception.auth.InvalidCredentialsException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.service.mobile.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    RateLimitService rateLimitService;

    private static final UUID USER_ID = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final String BASE   = "/api/v1/mobile/users";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    // Sets authentication principal to UUID — required for @CurrentUser to resolve correctly
    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private UserProfileResponse stubProfile() {
        return UserProfileResponse.builder()
                .id(USER_ID).name("Nicat").email("nicat@gmail.com")
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .points(0).streak(0).isVerified(true)
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // GET /me
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /me")
    class GetMyProfile {

        @Test
        @DisplayName("200 returns profile with correct JSON structure")
        void getProfile_success() throws Exception {
            when(userService.getMyProfile(USER_ID)).thenReturn(stubProfile());

            mockMvc.perform(get(BASE + "/me").with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.data.email").value("nicat@gmail.com"))
                    .andExpect(jsonPath("$.data.name").value("Nicat"))
                    .andExpect(jsonPath("$.data.verified").value(true));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getProfile_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE + "/me"))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).getMyProfile(any());
        }

        @Test
        @DisplayName("404 when user not found")
        void getProfile_notFound() throws Exception {
            when(userService.getMyProfile(USER_ID)).thenThrow(new UserNotFoundException());

            mockMvc.perform(get(BASE + "/me").with(asUser()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PATCH /me
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /me")
    class UpdateProfile {

        @Test
        @DisplayName("200 and returns updated profile on valid request")
        void updateProfile_success() throws Exception {
            when(userService.updateProfile(eq(USER_ID), any())).thenReturn(stubProfile());

            mockMvc.perform(patch(BASE + "/me")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "New Name", "country": "Azerbaijan" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile updated"));
        }

        @Test
        @DisplayName("200 with empty body — all fields are optional")
        void updateProfile_emptyBody() throws Exception {
            when(userService.updateProfile(eq(USER_ID), any())).thenReturn(stubProfile());

            mockMvc.perform(patch(BASE + "/me")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 when name is too short (under 2 characters)")
        void updateProfile_nameTooShort() throws Exception {
            mockMvc.perform(patch(BASE + "/me")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "A" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("400 when name exceeds 60 characters")
        void updateProfile_nameTooLong() throws Exception {
            String longName = "A".repeat(61);

            mockMvc.perform(patch(BASE + "/me")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"" + longName + "\" }"))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("400 when country exceeds 100 characters")
        void updateProfile_countryTooLong() throws Exception {
            String longCountry = "A".repeat(101);

            mockMvc.perform(patch(BASE + "/me")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"country\": \"" + longCountry + "\" }"))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateProfile(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void updateProfile_unauthenticated() throws Exception {
            mockMvc.perform(patch(BASE + "/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Nicat" }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 when user not found")
        void updateProfile_notFound() throws Exception {
            when(userService.updateProfile(eq(USER_ID), any()))
                    .thenThrow(new UserNotFoundException());

            mockMvc.perform(patch(BASE + "/me")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "New Name" }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // POST /me/avatar
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /me/avatar")
    class UploadAvatar {

        @Test
        @DisplayName("200 on valid image upload")
        void uploadAvatar_success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[1024]);

            when(userService.uploadAvatar(eq(USER_ID), any())).thenReturn(stubProfile());

            mockMvc.perform(multipart(BASE + "/me/avatar")
                            .file(file)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Avatar updated"));
        }

        @Test
        @DisplayName("400 when service rejects non-image file")
        void uploadAvatar_notImage() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[1024]);

            when(userService.uploadAvatar(eq(USER_ID), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed"));

            mockMvc.perform(multipart(BASE + "/me/avatar")
                            .file(file)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when service rejects file exceeding 5 MB")
        void uploadAvatar_tooLarge() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

            when(userService.uploadAvatar(eq(USER_ID), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar must not exceed 5 MB"));

            mockMvc.perform(multipart(BASE + "/me/avatar")
                            .file(file)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("500 when S3 upload fails unexpectedly")
        void uploadAvatar_s3Failure() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[1024]);

            when(userService.uploadAvatar(eq(USER_ID), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file"));

            mockMvc.perform(multipart(BASE + "/me/avatar")
                            .file(file)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void uploadAvatar_unauthenticated() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[1024]);

            mockMvc.perform(multipart(BASE + "/me/avatar")
                            .file(file).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // POST /me/change-password
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /me/change-password")
    class ChangePassword {

        @Test
        @DisplayName("200 with correct message on successful password change")
        void changePassword_success() throws Exception {
            doNothing().when(userService).changePassword(eq(USER_ID), any());

            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "oldPass123",
                                      "newPassword": "newPass123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password changed. Please log in again."));
        }

        @Test
        @DisplayName("400 when currentPassword is blank")
        void changePassword_blankCurrentPassword() throws Exception {
            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "",
                                      "newPassword": "newPass123!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("400 when newPassword is blank")
        void changePassword_blankNewPassword() throws Exception {
            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "oldPass123",
                                      "newPassword": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("400 when newPassword is too short (under 8 characters)")
        void changePassword_newPasswordTooShort() throws Exception {
            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "oldPass123",
                                      "newPassword": "short"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("400 when currentPassword fields is missing")
        void changePassword_missingCurrentPassword() throws Exception {
            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "newPassword": "newPass123!" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 when current password is wrong")
        void changePassword_wrongCurrentPassword() throws Exception {
            doThrow(new InvalidCredentialsException())
                    .when(userService).changePassword(eq(USER_ID), any());

            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "wrongPass",
                                      "newPassword": "newPass123!"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void changePassword_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE + "/me/change-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "oldPass123",
                                      "newPassword": "newPass123!"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE /me
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /me")
    class DeleteAccount {

        @Test
        @DisplayName("200 with correct message on successful deletion")
        void deleteAccount_success() throws Exception {
            doNothing().when(userService).deleteAccount(USER_ID);

            mockMvc.perform(delete(BASE + "/me")
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account deleted"));
        }

        @Test
        @DisplayName("404 when user not found")
        void deleteAccount_notFound() throws Exception {
            doThrow(new UserNotFoundException()).when(userService).deleteAccount(USER_ID);

            mockMvc.perform(delete(BASE + "/me")
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void deleteAccount_unauthenticated() throws Exception {
            mockMvc.perform(delete(BASE + "/me").with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).deleteAccount(any());
        }
    }
}