package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.exception.AccessDeniedException;
import com.dway.dwaybackend.common.exception.category.CategoryNameExistsException;
import com.dway.dwaybackend.common.exception.category.CategoryNotFoundException;
import com.dway.dwaybackend.dto.response.category.CategoryResponse;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.service.mobile.CategoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(CategoryController.class)
@DisplayName("CategoryController")
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CategoryService categoryService;
    @MockitoBean RateLimitService rateLimitService;

    private static final UUID USER_ID     = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID CATEGORY_ID = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");
    private static final String BASE      = "/api/v1/mobile/categories";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private CategoryResponse stubCategory() {
        return CategoryResponse.builder()
                .id(CATEGORY_ID)
                .userId(USER_ID)
                .name("Work")
                .icon("briefcase")
                .color("#FF5733")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CategoryResponse stubDefaultCategory() {
        return CategoryResponse.builder()
                .id(UUID.randomUUID())
                .userId(null)
                .name("General")
                .icon("star")
                .color("#FFFFFF")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // GET /categories
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET / — getAllCategories")
    class GetAllCategories {

        @Test
        @DisplayName("200 returns list of default and own categories")
        void getAllCategories_success() throws Exception {
            when(categoryService.getAllCategories(USER_ID))
                    .thenReturn(List.of(stubDefaultCategory(), stubCategory()));

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("200 returns empty list when user has no categories")
        void getAllCategories_empty() throws Exception {
            when(categoryService.getAllCategories(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getAllCategories_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());

            verify(categoryService, never()).getAllCategories(any());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // POST /categories
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST / — createCategory")
    class CreateCategory {

        @Test
        @DisplayName("200 returns created category with message")
        void createCategory_success() throws Exception {
            when(categoryService.createCategory(eq(USER_ID), any())).thenReturn(stubCategory());

            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Work",
                                      "icon": "briefcase",
                                      "color": "#FF5733"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Category created"))
                    .andExpect(jsonPath("$.data.id").value(CATEGORY_ID.toString()))
                    .andExpect(jsonPath("$.data.name").value("Work"));
        }

        @Test
        @DisplayName("400 when name is blank")
        void createCategory_blankName() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "", "icon": "briefcase", "color": "#FF5733" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(), any());
        }

        @Test
        @DisplayName("400 when name is missing")
        void createCategory_missingName() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "icon": "briefcase", "color": "#FF5733" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(), any());
        }

        @Test
        @DisplayName("400 when icon is blank")
        void createCategory_blankIcon() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Work", "icon": "", "color": "#FF5733" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(), any());
        }

        @Test
        @DisplayName("400 when icon is missing")
        void createCategory_missingIcon() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Work", "color": "#FF5733" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(), any());
        }

        @Test
        @DisplayName("400 when color is blank")
        void createCategory_blankColor() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Work", "icon": "briefcase", "color": "" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(), any());
        }

        @Test
        @DisplayName("400 when color is missing")
        void createCategory_missingColor() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Work", "icon": "briefcase" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(), any());
        }

        @Test
        @DisplayName("409 when category name already exists for this user")
        void createCategory_duplicateName() throws Exception {
            when(categoryService.createCategory(eq(USER_ID), any()))
                    .thenThrow(new CategoryNameExistsException());

            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Work", "icon": "briefcase", "color": "#FF5733" }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void createCategory_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Work", "icon": "briefcase", "color": "#FF5733" }
                                    """))
                    .andExpect(status().isUnauthorized());

            verify(categoryService, never()).createCategory(any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PATCH /categories/{id}
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /{id} — updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("200 returns updated category with message")
        void updateCategory_success() throws Exception {
            when(categoryService.updateCategory(eq(USER_ID), eq(CATEGORY_ID), any()))
                    .thenReturn(stubCategory());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Updated Work", "color": "#00FF00" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Category updated"))
                    .andExpect(jsonPath("$.data.id").value(CATEGORY_ID.toString()));
        }

        @Test
        @DisplayName("200 with empty body — all fields are optional (PATCH semantics)")
        void updateCategory_emptyBody() throws Exception {
            when(categoryService.updateCategory(eq(USER_ID), eq(CATEGORY_ID), any()))
                    .thenReturn(stubCategory());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("404 when category not found")
        void updateCategory_notFound() throws Exception {
            when(categoryService.updateCategory(eq(USER_ID), eq(CATEGORY_ID), any()))
                    .thenThrow(new CategoryNotFoundException());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Updated" }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when trying to update a default or another user's category")
        void updateCategory_accessDenied() throws Exception {
            when(categoryService.updateCategory(eq(USER_ID), eq(CATEGORY_ID), any()))
                    .thenThrow(new AccessDeniedException());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Hacked" }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("409 when new name is already taken by this user")
        void updateCategory_duplicateName() throws Exception {
            when(categoryService.updateCategory(eq(USER_ID), eq(CATEGORY_ID), any()))
                    .thenThrow(new CategoryNameExistsException());

            mockMvc.perform(patch(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Personal" }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
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

            verify(categoryService, never()).updateCategory(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE /categories/{id}
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /{id} — deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("200 returns success message with null data")
        void deleteCategory_success() throws Exception {
            doNothing().when(categoryService).deleteCategory(USER_ID, CATEGORY_ID);

            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Category deleted"));
        }

        @Test
        @DisplayName("404 when category not found")
        void deleteCategory_notFound() throws Exception {
            doThrow(new CategoryNotFoundException())
                    .when(categoryService).deleteCategory(USER_ID, CATEGORY_ID);

            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("403 when trying to delete a default or another user's category")
        void deleteCategory_accessDenied() throws Exception {
            doThrow(new AccessDeniedException())
                    .when(categoryService).deleteCategory(USER_ID, CATEGORY_ID);

            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void deleteCategory_unauthenticated() throws Exception {
            mockMvc.perform(delete(BASE + "/" + CATEGORY_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(categoryService, never()).deleteCategory(any(), any());
        }
    }
}