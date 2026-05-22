package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.exception.AccessDeniedException;
import com.dway.dwaybackend.common.exception.category.CategoryNameExistsException;
import com.dway.dwaybackend.common.exception.category.CategoryNotFoundException;
import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.category.CategoryResponse;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.admin.AdminCategoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoryController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminCategoryController")
class AdminCategoryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminCategoryService adminCategoryService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID ADMIN_ID    = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID     = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID CATEGORY_ID = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");
    private static final String BASE      = "/api/v1/admin/categories";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

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

    private CategoryResponse stubDefault() {
        return CategoryResponse.builder()
                .id(CATEGORY_ID)
                .userId(null)
                .name("General")
                .icon("star")
                .color("#FFFFFF")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET / — getAllCategories")
    class GetAllCategories {

        @Test
        @DisplayName("200 returns list of default categories")
        void getAllCategories_success() throws Exception {
            when(adminCategoryService.getAllDefaultCategories()).thenReturn(List.of(stubDefault()));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("General"))
                    .andExpect(jsonPath("$.data[0].default").value(true));
        }

        @Test
        @DisplayName("200 returns empty list when no default categories exist")
        void getAllCategories_empty() throws Exception {
            when(adminCategoryService.getAllDefaultCategories()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("403 when called by regular user")
        void getAllCategories_forbiddenForUser() throws Exception {
            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isForbidden());

            verify(adminCategoryService, never()).getAllDefaultCategories();
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getAllCategories_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());

            verify(adminCategoryService, never()).getAllDefaultCategories();
        }
    }

    @Nested
    @DisplayName("POST / — createDefaultCategory")
    class CreateDefaultCategory {

        @Test
        @DisplayName("200 returns created default category with message")
        void createDefaultCategory_success() throws Exception {
            when(adminCategoryService.createDefaultCategory(any())).thenReturn(stubDefault());

            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "General",
                                      "icon": "star",
                                      "color": "#FFFFFF"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Category created"))
                    .andExpect(jsonPath("$.data.id").value(CATEGORY_ID.toString()))
                    .andExpect(jsonPath("$.data.default").value(true));
        }

        @Test
        @DisplayName("400 when name is blank")
        void createDefaultCategory_blankName() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "", "icon": "star", "color": "#FFFFFF" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminCategoryService, never()).createDefaultCategory(any());
        }

        @Test
        @DisplayName("400 when name is missing")
        void createDefaultCategory_missingName() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "icon": "star", "color": "#FFFFFF" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminCategoryService, never()).createDefaultCategory(any());
        }

        @Test
        @DisplayName("400 when icon is blank")
        void createDefaultCategory_blankIcon() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "General", "icon": "", "color": "#FFFFFF" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminCategoryService, never()).createDefaultCategory(any());
        }

        @Test
        @DisplayName("400 when color is blank")
        void createDefaultCategory_blankColor() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "General", "icon": "star", "color": "" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminCategoryService, never()).createDefaultCategory(any());
        }

        @Test
        @DisplayName("409 when default category name already exists")
        void createDefaultCategory_duplicateName() throws Exception {
            when(adminCategoryService.createDefaultCategory(any()))
                    .thenThrow(new CategoryNameExistsException());

            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "General", "icon": "star", "color": "#FFFFFF" }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by regular user")
        void createDefaultCategory_forbiddenForUser() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "General", "icon": "star", "color": "#FFFFFF" }
                                    """))
                    .andExpect(status().isForbidden());

            verify(adminCategoryService, never()).createDefaultCategory(any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void createDefaultCategory_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "General", "icon": "star", "color": "#FFFFFF" }
                                    """))
                    .andExpect(status().isUnauthorized());

            verify(adminCategoryService, never()).createDefaultCategory(any());
        }
    }

    @Nested
    @DisplayName("PATCH /{id} — updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("200 returns updated category with message")
        void updateCategory_success() throws Exception {
            when(adminCategoryService.updateDefaultCategory(eq(CATEGORY_ID), any())).thenReturn(stubDefault());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Updated", "color": "#000000" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Category updated"));
        }

        @Test
        @DisplayName("200 with empty body — all fields are optional")
        void updateCategory_emptyBody() throws Exception {
            when(adminCategoryService.updateDefaultCategory(eq(CATEGORY_ID), any())).thenReturn(stubDefault());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("404 when category not found")
        void updateCategory_notFound() throws Exception {
            when(adminCategoryService.updateDefaultCategory(eq(CATEGORY_ID), any()))
                    .thenThrow(new CategoryNotFoundException());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Updated" }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when category is user-created")
        void updateCategory_userCreatedCategory() throws Exception {
            when(adminCategoryService.updateDefaultCategory(eq(CATEGORY_ID), any()))
                    .thenThrow(new AccessDeniedException());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Updated" }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("409 when new name is already taken")
        void updateCategory_duplicateName() throws Exception {
            when(adminCategoryService.updateDefaultCategory(eq(CATEGORY_ID), any()))
                    .thenThrow(new CategoryNameExistsException());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Health" }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by regular user")
        void updateCategory_forbiddenForUser() throws Exception {
            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Hacked" }
                                    """))
                    .andExpect(status().isForbidden());

            verify(adminCategoryService, never()).updateDefaultCategory(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void updateCategory_unauthenticated() throws Exception {
            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Updated" }
                                    """))
                    .andExpect(status().isUnauthorized());

            verify(adminCategoryService, never()).updateDefaultCategory(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /{id} — deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("200 returns success message")
        void deleteCategory_success() throws Exception {
            doNothing().when(adminCategoryService).deleteDefaultCategory(CATEGORY_ID);

            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Category deleted"));
        }

        @Test
        @DisplayName("404 when category not found")
        void deleteCategory_notFound() throws Exception {
            doThrow(new CategoryNotFoundException())
                    .when(adminCategoryService).deleteDefaultCategory(CATEGORY_ID);

            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when category is user-created")
        void deleteCategory_userCreatedCategory() throws Exception {
            doThrow(new AccessDeniedException())
                    .when(adminCategoryService).deleteDefaultCategory(CATEGORY_ID);

            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when called by regular user")
        void deleteCategory_forbiddenForUser() throws Exception {
            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(adminCategoryService, never()).deleteDefaultCategory(any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void deleteCategory_unauthenticated() throws Exception {
            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(adminCategoryService, never()).deleteDefaultCategory(any());
        }
    }
}