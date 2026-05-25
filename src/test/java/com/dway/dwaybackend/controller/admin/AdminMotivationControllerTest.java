package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.exception.motivation.MotivationNotFoundException;
import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.admin.AdminMotivationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
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

@WebMvcTest(AdminMotivationController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminMotivationController")
class AdminMotivationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminMotivationService adminMotivationService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID ID     = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String BASE = "/api/v1/admin/motivations";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private MotivationResponse stub() {
        return MotivationResponse.builder()
                .id(ID)
                .quote("Do or do not.")
                .author("Yoda")
                .lastShownDate(LocalDate.now().minusDays(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Page<MotivationResponse> page(List<MotivationResponse> items) {
        return new PageImpl<>(items, PageRequest.of(0, 20), items.size());
    }

    @Nested
    @DisplayName("GET / — getAllMotivations")
    class GetAllMotivations {

        @Test
        @DisplayName("200 — returns paginated list with correct fields")
        void success_returnsPage() throws Exception {
            when(adminMotivationService.getAllMotivations(any())).thenReturn(page(List.of(stub())));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].quote").value("Do or do not."))
                    .andExpect(jsonPath("$.data.content[0].lastShownDate").isNotEmpty());
        }

        @Test
        @DisplayName("200 — returns empty content array when pool is empty")
        void success_emptyPool() throws Exception {
            when(adminMotivationService.getAllMotivations(any()))
                    .thenReturn(page(Collections.emptyList()));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("200 — lastShownDate absent from JSON when null (never-shown motivation)")
        void success_lastShownDateAbsentWhenNull() throws Exception {
            MotivationResponse neverShown = MotivationResponse.builder()
                    .id(ID).quote("Q").author("A")
                    .lastShownDate(null)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(adminMotivationService.getAllMotivations(any())).thenReturn(page(List.of(neverShown)));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].lastShownDate").doesNotExist());
        }

        @Test
        @DisplayName("401 — no auth header")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());

            verify(adminMotivationService, never()).getAllMotivations(any());
        }

        @Test
        @DisplayName("403 — regular ROLE_USER cannot access admin endpoint")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isForbidden());

            verify(adminMotivationService, never()).getAllMotivations(any());
        }
    }

    @Nested
    @DisplayName("GET /{id} — getMotivationById")
    class GetMotivationById {

        @Test
        @DisplayName("200 — returns the motivation with all fields including lastShownDate")
        void success_returnsMotivation() throws Exception {
            when(adminMotivationService.getMotivationById(ID)).thenReturn(stub());

            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.quote").value("Do or do not."))
                    .andExpect(jsonPath("$.data.author").value("Yoda"))
                    .andExpect(jsonPath("$.data.lastShownDate").isNotEmpty());
        }

        @Test
        @DisplayName("404 — when motivation does not exist")
        void notFound_returns404() throws Exception {
            when(adminMotivationService.getMotivationById(ID))
                    .thenThrow(new MotivationNotFoundException());

            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("401 — unauthenticated request")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID))
                    .andExpect(status().isUnauthorized());

            verify(adminMotivationService, never()).getMotivationById(any());
        }

        @Test
        @DisplayName("403 — regular user")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID).with(asUser()))
                    .andExpect(status().isForbidden());

            verify(adminMotivationService, never()).getMotivationById(any());
        }
    }

    @Nested
    @DisplayName("POST / — createMotivation")
    class CreateMotivation {

        private static final String VALID_FULL = """
                { "quote": "Do or do not.", "author": "Yoda" }
                """;

        @Test
        @DisplayName("200 — creates motivation with quote and author")
        void success_withAuthor() throws Exception {
            when(adminMotivationService.createMotivation(any())).thenReturn(stub());

            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_FULL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Motivation created"))
                    .andExpect(jsonPath("$.data.id").value(ID.toString()));
        }

        @Test
        @DisplayName("200 — creates motivation without author (optional field)")
        void success_withoutAuthor() throws Exception {
            when(adminMotivationService.createMotivation(any())).thenReturn(stub());

            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Some quote" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("200 — service is called exactly once")
        void success_serviceCalledOnce() throws Exception {
            when(adminMotivationService.createMotivation(any())).thenReturn(stub());

            mockMvc.perform(post(BASE)
                    .with(asAdmin()).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_FULL));

            verify(adminMotivationService, times(1)).createMotivation(any());
        }

        @Test
        @DisplayName("400 — quote is blank")
        void validation_blankQuote() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "" }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminMotivationService, never()).createMotivation(any());
        }

        @Test
        @DisplayName("400 — quote is whitespace only")
        void validation_whitespaceQuote() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "   " }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(adminMotivationService, never()).createMotivation(any());
        }

        @Test
        @DisplayName("400 — quote field is missing entirely")
        void validation_missingQuote() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(adminMotivationService, never()).createMotivation(any());
        }

        @Test
        @DisplayName("400 — body is empty")
        void validation_emptyBody() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 — unauthenticated request")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_FULL))
                    .andExpect(status().isUnauthorized());

            verify(adminMotivationService, never()).createMotivation(any());
        }

        @Test
        @DisplayName("403 — regular user cannot upload motivations")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_FULL))
                    .andExpect(status().isForbidden());

            verify(adminMotivationService, never()).createMotivation(any());
        }
    }

    @Nested
    @DisplayName("PATCH /{id} — updateMotivation")
    class UpdateMotivation {

        @Test
        @DisplayName("200 — updates quote and author")
        void success_updatesBothFields() throws Exception {
            when(adminMotivationService.updateMotivation(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Updated quote", "author": "New Author" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Motivation updated"));
        }

        @Test
        @DisplayName("200 — updates quote only")
        void success_updatesQuoteOnly() throws Exception {
            when(adminMotivationService.updateMotivation(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Fixed typo" }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 — updates author only")
        void success_updatesAuthorOnly() throws Exception {
            when(adminMotivationService.updateMotivation(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "author": "Corrected Author" }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 — empty body accepted (PATCH — all fields optional)")
        void success_emptyBodyAccepted() throws Exception {
            when(adminMotivationService.updateMotivation(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 — response contains updated data")
        void success_responseContainsData() throws Exception {
            when(adminMotivationService.updateMotivation(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Updated" }
                                    """))
                    .andExpect(jsonPath("$.data.id").value(ID.toString()));
        }

        @Test
        @DisplayName("404 — motivation does not exist")
        void notFound_returns404() throws Exception {
            when(adminMotivationService.updateMotivation(eq(ID), any()))
                    .thenThrow(new MotivationNotFoundException());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Updated" }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 — unauthenticated request")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Updated" }
                                    """))
                    .andExpect(status().isUnauthorized());

            verify(adminMotivationService, never()).updateMotivation(any(), any());
        }

        @Test
        @DisplayName("403 — regular user cannot update motivations")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "quote": "Updated" }
                                    """))
                    .andExpect(status().isForbidden());

            verify(adminMotivationService, never()).updateMotivation(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /{id} — deleteMotivation")
    class DeleteMotivation {

        @Test
        @DisplayName("200 — deletes and returns success message")
        void success_deletesMotivation() throws Exception {
            doNothing().when(adminMotivationService).deleteMotivation(ID);

            mockMvc.perform(delete(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Motivation deleted"));
        }

        @Test
        @DisplayName("200 — data field absent after delete")
        void success_dataAbsent() throws Exception {
            doNothing().when(adminMotivationService).deleteMotivation(ID);

            mockMvc.perform(delete(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("200 — service called exactly once")
        void success_serviceCalledOnce() throws Exception {
            doNothing().when(adminMotivationService).deleteMotivation(ID);

            mockMvc.perform(delete(BASE + "/" + ID)
                    .with(asAdmin()).with(csrf()));

            verify(adminMotivationService, times(1)).deleteMotivation(ID);
        }

        @Test
        @DisplayName("404 — motivation does not exist")
        void notFound_returns404() throws Exception {
            doThrow(new MotivationNotFoundException())
                    .when(adminMotivationService).deleteMotivation(ID);

            mockMvc.perform(delete(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("401 — unauthenticated request")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(adminMotivationService, never()).deleteMotivation(any());
        }

        @Test
        @DisplayName("403 — regular user cannot delete motivations")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());

            verify(adminMotivationService, never()).deleteMotivation(any());
        }
    }
}