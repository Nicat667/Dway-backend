package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.exception.auth.*;
import com.dway.dwaybackend.common.exception.verification.CodeRecentlySentException;
import com.dway.dwaybackend.common.exception.verification.InvalidVerificationCodeException;
import com.dway.dwaybackend.dto.response.auth.AuthResponse;
import com.dway.dwaybackend.dto.response.auth.RefreshTokenResponse;
import com.dway.dwaybackend.dto.response.auth.UserResponse;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.service.mobile.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    RateLimitService  rateLimitService;

    private static final String BASE = "/api/v1/auth";

    @BeforeEach
    void allowRateLimit() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    private AuthResponse stubAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(UserResponse.builder()
                        .id(UUID.randomUUID())
                        .name("Nicat")
                        .email("nicat@gmail.com")
                        .plan(Plan.FREE)
                        .roles(Set.of(Role.USER))
                        .points(0)
                        .streak(0)
                        .isVerified(true)
                        .build())
                .build();
    }

    @Nested
    @DisplayName("POST /register")
    class Register {

        @Test
        @DisplayName("201 on valid request")
        @WithMockUser
        void register_success() throws Exception {
            doNothing().when(authService).register(any());

            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Nicat",
                                      "email": "nicat@gmail.com",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Registration successful. Please check your email for a verification code."));
        }

        @Test
        @DisplayName("400 when name is blank")
        @WithMockUser
        void register_blankName() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "",
                                      "email": "nicat@gmail.com",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when email is invalid")
        @WithMockUser
        void register_invalidEmail() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Nicat",
                                      "email": "not-an-email",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when password too short")
        @WithMockUser
        void register_shortPassword() throws Exception {
            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Nicat",
                                      "email": "nicat@gmail.com",
                                      "password": "short"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 when email already exists")
        @WithMockUser
        void register_emailTaken() throws Exception {
            doThrow(new EmailAlreadyExistsException()).when(authService).register(any());

            mockMvc.perform(post(BASE + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Nicat",
                                      "email": "nicat@gmail.com",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("200 on valid code")
        @WithMockUser
        void verifyEmail_success() throws Exception {
            when(authService.verifyEmail(any())).thenReturn(stubAuthResponse());

            mockMvc.perform(post(BASE + "/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": "847291"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
        }

        @Test
        @DisplayName("400 when code is blank")
        @WithMockUser
        void verifyEmail_blankCode() throws Exception {
            mockMvc.perform(post(BASE + "/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when code is invalid")
        @WithMockUser
        void verifyEmail_invalidCode() throws Exception {
            doThrow(new InvalidVerificationCodeException()).when(authService).verifyEmail(any());

            mockMvc.perform(post(BASE + "/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": "000000"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("404 when user not found")
        @WithMockUser
        void verifyEmail_userNotFound() throws Exception {
            doThrow(new UserNotFoundException()).when(authService).verifyEmail(any());

            mockMvc.perform(post(BASE + "/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "ghost@gmail.com",
                                      "code": "847291"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /resend-code")
    class ResendCode {

        @Test
        @DisplayName("200 on valid request")
        @WithMockUser
        void resendCode_success() throws Exception {
            doNothing().when(authService).resendCode(any());

            mockMvc.perform(post(BASE + "/resend-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "nicat@gmail.com" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("400 when email is invalid")
        @WithMockUser
        void resendCode_invalidEmail() throws Exception {
            mockMvc.perform(post(BASE + "/resend-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "bad" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("429 when code sent too recently")
        @WithMockUser
        void resendCode_tooSoon() throws Exception {
            doThrow(new CodeRecentlySentException()).when(authService).resendCode(any());

            mockMvc.perform(post(BASE + "/resend-code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "nicat@gmail.com" }
                                    """))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /login")
    class Login {

        @Test
        @DisplayName("200 with tokens on valid credentials")
        @WithMockUser
        void login_success() throws Exception {
            when(authService.login(any())).thenReturn(stubAuthResponse());

            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.user.email").value("nicat@gmail.com"));
        }

        @Test
        @DisplayName("400 when email is blank")
        @WithMockUser
        void login_blankEmail() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 on wrong credentials")
        @WithMockUser
        void login_wrongCredentials() throws Exception {
            doThrow(new InvalidCredentialsException()).when(authService).login(any());

            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "password": "wrongpass"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when email not verified")
        @WithMockUser
        void login_notVerified() throws Exception {
            doThrow(new EmailNotVerifiedException()).when(authService).login(any());

            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when user is banned")
        @WithMockUser
        void login_banned() throws Exception {
            doThrow(new UserBannedException()).when(authService).login(any());

            mockMvc.perform(post(BASE + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /refresh")
    class Refresh {

        @Test
        @DisplayName("200 with new access token")
        @WithMockUser
        void refresh_success() throws Exception {
            when(authService.refresh(any())).thenReturn(
                    RefreshTokenResponse.builder().accessToken("new-access-token").build());

            mockMvc.perform(post(BASE + "/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "refreshToken": "some-refresh-token" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("400 when refreshToken is blank")
        @WithMockUser
        void refresh_blankToken() throws Exception {
            mockMvc.perform(post(BASE + "/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "refreshToken": "" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 when token is invalid or expired")
        @WithMockUser
        void refresh_invalidToken() throws Exception {
            doThrow(new InvalidRefreshTokenException()).when(authService).refresh(any());

            mockMvc.perform(post(BASE + "/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "refreshToken": "expired-token" }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /logout")
    class Logout {

        @Test
        @DisplayName("200 on valid token")
        @WithMockUser
        void logout_success() throws Exception {
            doNothing().when(authService).logout(any());

            mockMvc.perform(post(BASE + "/logout")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "refreshToken": "some-refresh-token" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("400 when refreshToken is blank")
        @WithMockUser
        void logout_blankToken() throws Exception {
            mockMvc.perform(post(BASE + "/logout")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "refreshToken": "" }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("200 regardless of whether email exists")
        @WithMockUser
        void forgotPassword_success() throws Exception {
            doNothing().when(authService).forgotPassword(any());

            mockMvc.perform(post(BASE + "/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "nicat@gmail.com" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("400 when email is invalid")
        @WithMockUser
        void forgotPassword_invalidEmail() throws Exception {
            mockMvc.perform(post(BASE + "/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "bad" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("429 when code sent too recently")
        @WithMockUser
        void forgotPassword_tooSoon() throws Exception {
            doThrow(new CodeRecentlySentException()).when(authService).forgotPassword(any());

            mockMvc.perform(post(BASE + "/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "nicat@gmail.com" }
                                    """))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /reset-password")
    class ResetPassword {

        @Test
        @DisplayName("200 on valid code and password")
        @WithMockUser
        void resetPassword_success() throws Exception {
            doNothing().when(authService).resetPassword(any());

            mockMvc.perform(post(BASE + "/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": "847291",
                                      "newPassword": "newpassword123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password reset successfully. Please log in."));
        }

        @Test
        @DisplayName("400 when code length is wrong")
        @WithMockUser
        void resetPassword_shortCode() throws Exception {
            mockMvc.perform(post(BASE + "/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": "123",
                                      "newPassword": "newpassword123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when new password too short")
        @WithMockUser
        void resetPassword_shortPassword() throws Exception {
            mockMvc.perform(post(BASE + "/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": "847291",
                                      "newPassword": "short"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when code is invalid or expired")
        @WithMockUser
        void resetPassword_invalidCode() throws Exception {
            doThrow(new InvalidVerificationCodeException()).when(authService).resetPassword(any());

            mockMvc.perform(post(BASE + "/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "nicat@gmail.com",
                                      "code": "000000",
                                      "newPassword": "newpassword123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("404 when user not found")
        @WithMockUser
        void resetPassword_userNotFound() throws Exception {
            doThrow(new UserNotFoundException()).when(authService).resetPassword(any());

            mockMvc.perform(post(BASE + "/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "ghost@gmail.com",
                                      "code": "847291",
                                      "newPassword": "newpassword123"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}