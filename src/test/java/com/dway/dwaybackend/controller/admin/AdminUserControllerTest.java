package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.exception.AccessDeniedException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.user.AdminUserResponse;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtAuthEntryPoint;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.admin.AdminUserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthEntryPoint.class})
@DisplayName("AdminUserController")
class AdminUserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean
    AdminUserService adminUserService;
    @MockitoBean
    RateLimitService rateLimitService;
    @MockitoBean
    JwtUtil jwtUtil;

    private static final UUID USER_ID  = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID ADMIN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final String BASE   = "/api/v1/admin/users";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    // UUID principal — needed for @CurrentUser UUID adminId to resolve correctly
    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                ADMIN_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private AdminUserResponse stubResponse() {
        return AdminUserResponse.builder()
                .id(USER_ID).name("Nicat").email("nicat@gmail.com")
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .isBanned(false).isVerified(true)
                .build();
    }

    @Nested
    @DisplayName("GET /")
    class GetAllUsers {

        @Test
        @DisplayName("200 returns paginated response with totalElements")
        void getAllUsers_success() throws Exception {
            Page<AdminUserResponse> page = new PageImpl<>(
                    List.of(stubResponse()), PageRequest.of(0, 20), 1);

            when(adminUserService.getAllUsers(any())).thenReturn(page);

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].email").value("nicat@gmail.com"));
        }

        @Test
        @DisplayName("200 returns empty page when no users exist")
        void getAllUsers_emptyPage() throws Exception {
            when(adminUserService.getAllUsers(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("403 when called by a regular user")
        void getAllUsers_forbiddenForUser() throws Exception {
            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isForbidden());

            verify(adminUserService, never()).getAllUsers(any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getAllUsers_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());

            verify(adminUserService, never()).getAllUsers(any());
        }
    }

    @Nested
    @DisplayName("GET /{userId}")
    class GetUserById {

        @Test
        @DisplayName("200 returns user with full admin details")
        void getUserById_success() throws Exception {
            when(adminUserService.getUserById(USER_ID)).thenReturn(stubResponse());

            mockMvc.perform(get(BASE + "/" + USER_ID).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.data.email").value("nicat@gmail.com"))
                    .andExpect(jsonPath("$.data.banned").value(false));
        }

        @Test
        @DisplayName("404 when user not found")
        void getUserById_notFound() throws Exception {
            when(adminUserService.getUserById(USER_ID)).thenThrow(new UserNotFoundException());

            mockMvc.perform(get(BASE + "/" + USER_ID).with(asAdmin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by a regular user")
        void getUserById_forbiddenForUser() throws Exception {
            mockMvc.perform(get(BASE + "/" + USER_ID).with(asUser()))
                    .andExpect(status().isForbidden());

            verify(adminUserService, never()).getUserById(any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getUserById_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE + "/" + USER_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /{userId}/ban")
    class BanUser {

        @Test
        @DisplayName("200 with correct message when admin bans a user")
        void banUser_success() throws Exception {
            AdminUserResponse banned = stubResponse();
            banned.setBanned(true);
            banned.setBannedAt(LocalDateTime.now());

            when(adminUserService.banUser(eq(USER_ID), eq(ADMIN_ID))).thenReturn(banned);

            mockMvc.perform(post(BASE + "/" + USER_ID + "/ban")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User banned"))
                    .andExpect(jsonPath("$.data.banned").value(true));
        }

        @Test
        @DisplayName("403 when admin tries to ban themselves")
        void banUser_selfBan() throws Exception {
            when(adminUserService.banUser(eq(ADMIN_ID), eq(ADMIN_ID)))
                    .thenThrow(new AccessDeniedException());

            mockMvc.perform(post(BASE + "/" + ADMIN_ID + "/ban")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when admin tries to ban another admin")
        void banUser_banAnotherAdmin() throws Exception {
            UUID otherAdmin = UUID.fromString("88888888-8888-8888-8888-888888888888");

            when(adminUserService.banUser(eq(otherAdmin), eq(ADMIN_ID)))
                    .thenThrow(new AccessDeniedException());

            mockMvc.perform(post(BASE + "/" + otherAdmin + "/ban")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("404 when target user not found")
        void banUser_notFound() throws Exception {
            when(adminUserService.banUser(eq(USER_ID), eq(ADMIN_ID)))
                    .thenThrow(new UserNotFoundException());

            mockMvc.perform(post(BASE + "/" + USER_ID + "/ban")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by a regular user")
        void banUser_forbiddenForUser() throws Exception {
            mockMvc.perform(post(BASE + "/" + USER_ID + "/ban").with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(adminUserService, never()).banUser(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void banUser_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE + "/" + USER_ID + "/ban").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /{userId}/unban")
    class UnbanUser {

        @Test
        @DisplayName("200 with correct message when admin unbans a user")
        void unbanUser_success() throws Exception {
            when(adminUserService.unbanUser(eq(USER_ID), eq(ADMIN_ID))).thenReturn(stubResponse());

            mockMvc.perform(post(BASE + "/" + USER_ID + "/unban")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User unbanned"));
        }

        @Test
        @DisplayName("404 when user not found")
        void unbanUser_notFound() throws Exception {
            when(adminUserService.unbanUser(eq(USER_ID), eq(ADMIN_ID)))
                    .thenThrow(new UserNotFoundException());

            mockMvc.perform(post(BASE + "/" + USER_ID + "/unban")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by a regular user")
        void unbanUser_forbiddenForUser() throws Exception {
            mockMvc.perform(post(BASE + "/" + USER_ID + "/unban").with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(adminUserService, never()).unbanUser(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void unbanUser_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE + "/" + USER_ID + "/unban").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /{userId}/roles")
    class UpdateUserRoles {

        @Test
        @DisplayName("200 with correct message on successful role update")
        void updateRoles_success() throws Exception {
            when(adminUserService.updateUserRoles(eq(USER_ID), any(), eq(ADMIN_ID)))
                    .thenReturn(stubResponse());

            mockMvc.perform(put(BASE + "/" + USER_ID + "/roles")
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "roles": ["ADMIN"] }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Roles updated"));
        }

        @Test
        @DisplayName("400 when roles field is null")
        void updateRoles_nullRoles() throws Exception {
            mockMvc.perform(put(BASE + "/" + USER_ID + "/roles")
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "roles": null }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminUserService, never()).updateUserRoles(any(), any(), any());
        }

        @Test
        @DisplayName("400 when roles field is missing entirely")
        void updateRoles_missingRolesField() throws Exception {
            mockMvc.perform(put(BASE + "/" + USER_ID + "/roles")
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("403 when admin tries to update their own roles")
        void updateRoles_selfUpdate() throws Exception {
            when(adminUserService.updateUserRoles(eq(ADMIN_ID), any(), eq(ADMIN_ID)))
                    .thenThrow(new AccessDeniedException());

            mockMvc.perform(put(BASE + "/" + ADMIN_ID + "/roles")
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "roles": ["USER"] }
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("404 when target user not found")
        void updateRoles_notFound() throws Exception {
            when(adminUserService.updateUserRoles(eq(USER_ID), any(), eq(ADMIN_ID)))
                    .thenThrow(new UserNotFoundException());

            mockMvc.perform(put(BASE + "/" + USER_ID + "/roles")
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "roles": ["ADMIN"] }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 when called by a regular user")
        void updateRoles_forbiddenForUser() throws Exception {
            mockMvc.perform(put(BASE + "/" + USER_ID + "/roles")
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "roles": ["ADMIN"] }
                                    """))
                    .andExpect(status().isForbidden());

            verify(adminUserService, never()).updateUserRoles(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /{userId}")
    class DeleteUser {

        @Test
        @DisplayName("200 with correct message on successful deletion")
        void deleteUser_success() throws Exception {
            doNothing().when(adminUserService).deleteUser(eq(USER_ID), eq(ADMIN_ID));

            mockMvc.perform(delete(BASE + "/" + USER_ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User deleted"));
        }

        @Test
        @DisplayName("403 when admin tries to delete themselves")
        void deleteUser_selfDelete() throws Exception {
            doThrow(new AccessDeniedException())
                    .when(adminUserService).deleteUser(eq(ADMIN_ID), eq(ADMIN_ID));

            mockMvc.perform(delete(BASE + "/" + ADMIN_ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("404 when user not found")
        void deleteUser_notFound() throws Exception {
            doThrow(new UserNotFoundException())
                    .when(adminUserService).deleteUser(eq(USER_ID), eq(ADMIN_ID));

            mockMvc.perform(delete(BASE + "/" + USER_ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by a regular user")
        void deleteUser_forbiddenForUser() throws Exception {
            mockMvc.perform(delete(BASE + "/" + USER_ID).with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(adminUserService, never()).deleteUser(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void deleteUser_unauthenticated() throws Exception {
            mockMvc.perform(delete(BASE + "/" + USER_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(adminUserService, never()).deleteUser(any(), any());
        }
    }
}