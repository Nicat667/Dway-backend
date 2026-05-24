package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.exception.partner.PartnerNameExistsException;
import com.dway.dwaybackend.common.exception.partner.PartnerNotFoundException;
import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.admin.AdminPartnerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

@WebMvcTest(AdminPartnerController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminPartnerController")
class AdminPartnerControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminPartnerService adminPartnerService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID   ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String BASE = "/api/v1/admin/partners";

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

    private PartnerResponse stub() {
        return PartnerResponse.builder()
                .id(ID)
                .name("Bravo")
                .iconUrl("https://bucket.s3.amazonaws.com/partners/icon.png")
                .description("Big discount")
                .discountText("20% off")
                .promoCode("BRAVO20")
                .partnerUrl("https://bravo.az")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Page<PartnerResponse> page(List<PartnerResponse> items) {
        return new PageImpl<>(items, PageRequest.of(0, 20), items.size());
    }

    @Nested
    @DisplayName("GET / — getAllPartners")
    class GetAllPartners {

        @Test
        @DisplayName("200 — returns paginated list")
        void success_returnsList() throws Exception {
            when(adminPartnerService.getAllPartners(any(), any())).thenReturn(page(List.of(stub())));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].name").value("Bravo"))
                    .andExpect(jsonPath("$.data.content[0].isActive").value(true));
        }

        @Test
        @DisplayName("200 — with isActive=true filter")
        void success_withIsActiveFilter() throws Exception {
            when(adminPartnerService.getAllPartners(eq(true), any()))
                    .thenReturn(page(List.of(stub())));

            mockMvc.perform(get(BASE).param("isActive", "true").with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("200 — empty page when no partners exist")
        void success_emptyPage() throws Exception {
            when(adminPartnerService.getAllPartners(any(), any()))
                    .thenReturn(page(Collections.emptyList()));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("401 — no auth")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
            verify(adminPartnerService, never()).getAllPartners(any(), any());
        }

        @Test
        @DisplayName("403 — ROLE_USER cannot access admin endpoint")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(asUser())).andExpect(status().isForbidden());
            verify(adminPartnerService, never()).getAllPartners(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /{id} — getPartnerById")
    class GetPartnerById {

        @Test
        @DisplayName("200 — returns partner with all fields")
        void success_returnsPartner() throws Exception {
            when(adminPartnerService.getPartnerById(ID)).thenReturn(stub());

            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.name").value("Bravo"))
                    .andExpect(jsonPath("$.data.promoCode").value("BRAVO20"))
                    .andExpect(jsonPath("$.data.isActive").value(true));
        }

        @Test
        @DisplayName("404 — partner not found")
        void notFound_returns404() throws Exception {
            when(adminPartnerService.getPartnerById(ID)).thenThrow(new PartnerNotFoundException());

            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 — no auth")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — ROLE_USER cannot access")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID).with(asUser())).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST / — createPartner")
    class CreatePartner {
        
        private static final String VALID_JSON =
                "{\"name\":\"Bravo\",\"description\":\"desc\",\"discountText\":\"20% off\",\"promoCode\":\"BRAVO20\",\"partnerUrl\":\"https://bravo.az\"}";

        private MockMultipartFile iconFile() {
            return new MockMultipartFile("file", "icon.png", "image/png", new byte[]{1, 2, 3});
        }

        @Test
        @DisplayName("200 — creates partner and returns response")
        void success_createsPartner() throws Exception {
            when(adminPartnerService.createPartner(any(), any())).thenReturn(stub());

            mockMvc.perform(multipart(BASE)
                            .file(iconFile())
                            .param("data", VALID_JSON)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partner created"))
                    .andExpect(jsonPath("$.data.id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.name").value("Bravo"));
        }

        @Test
        @DisplayName("200 — service called exactly once")
        void success_serviceCalledOnce() throws Exception {
            when(adminPartnerService.createPartner(any(), any())).thenReturn(stub());

            mockMvc.perform(multipart(BASE)
                    .file(iconFile())
                    .param("data", VALID_JSON)
                    .with(asAdmin()).with(csrf()));

            verify(adminPartnerService, times(1)).createPartner(any(), any());
        }

        @Test
        @DisplayName("409 — partner name already exists")
        void conflict_nameExists() throws Exception {
            when(adminPartnerService.createPartner(any(), any()))
                    .thenThrow(new PartnerNameExistsException());

            mockMvc.perform(multipart(BASE)
                            .file(iconFile())
                            .param("data", VALID_JSON)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 — missing required field in data JSON")
        void validation_missingName() throws Exception {
            String missingName = "{\"description\":\"desc\",\"discountText\":\"20% off\",\"promoCode\":\"CODE\",\"partnerUrl\":\"https://x.az\"}";

            mockMvc.perform(multipart(BASE)
                            .file(iconFile())
                            .param("data", missingName)
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(adminPartnerService, never()).createPartner(any(), any());
        }

        @Test
        @DisplayName("400 — malformed JSON in data param")
        void validation_malformedJson() throws Exception {
            mockMvc.perform(multipart(BASE)
                            .file(iconFile())
                            .param("data", "not-valid-json{{{")
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(adminPartnerService, never()).createPartner(any(), any());
        }

        @Test
        @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(multipart(BASE)
                            .file(iconFile())
                            .param("data", VALID_JSON)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — ROLE_USER cannot create partners")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(multipart(BASE)
                            .file(iconFile())
                            .param("data", VALID_JSON)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /{id} — updatePartner")
    class UpdatePartner {

        @Test
        @DisplayName("200 — updates partner fields")
        void success_updatesPartner() throws Exception {
            when(adminPartnerService.updatePartner(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"Updated desc\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partner updated"));
        }

        @Test
        @DisplayName("200 — empty body accepted (PATCH — all fields optional)")
        void success_emptyBody() throws Exception {
            when(adminPartnerService.updatePartner(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("404 — partner not found")
        void notFound_returns404() throws Exception {
            when(adminPartnerService.updatePartner(eq(ID), any()))
                    .thenThrow(new PartnerNotFoundException());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"Updated\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 — new name already taken")
        void conflict_nameTaken() throws Exception {
            when(adminPartnerService.updatePartner(eq(ID), any()))
                    .thenThrow(new PartnerNameExistsException());

            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"TakenName\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — ROLE_USER cannot update")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /{id}/icon — updateIcon")
    class UpdateIcon {

        private MockMultipartFile iconFile() {
            return new MockMultipartFile("file", "new-icon.png", "image/png", new byte[]{4, 5, 6});
        }

        @Test
        @DisplayName("200 — replaces icon and returns response")
        void success_replacesIcon() throws Exception {
            when(adminPartnerService.updateIcon(eq(ID), any())).thenReturn(stub());

            mockMvc.perform(multipart(BASE + "/" + ID + "/icon")
                            .file(iconFile())
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partner icon updated"));
        }

        @Test
        @DisplayName("404 — partner not found")
        void notFound_returns404() throws Exception {
            when(adminPartnerService.updateIcon(eq(ID), any()))
                    .thenThrow(new PartnerNotFoundException());

            mockMvc.perform(multipart(BASE + "/" + ID + "/icon")
                            .file(iconFile())
                            .with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(multipart(BASE + "/" + ID + "/icon")
                            .file(iconFile())
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — ROLE_USER cannot replace icon")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(multipart(BASE + "/" + ID + "/icon")
                            .file(iconFile())
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /{id} — deletePartner")
    class DeletePartner {

        @Test
        @DisplayName("200 — deletes partner and returns success message")
        void success_deletesPartner() throws Exception {
            doNothing().when(adminPartnerService).deletePartner(ID);

            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Partner deleted"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("200 — service called exactly once")
        void success_serviceCalledOnce() throws Exception {
            doNothing().when(adminPartnerService).deletePartner(ID);

            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf()));

            verify(adminPartnerService, times(1)).deletePartner(ID);
        }

        @Test
        @DisplayName("404 — partner not found")
        void notFound_returns404() throws Exception {
            doThrow(new PartnerNotFoundException()).when(adminPartnerService).deletePartner(ID);

            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(csrf()))
                    .andExpect(status().isUnauthorized());
            verify(adminPartnerService, never()).deletePartner(any());
        }

        @Test
        @DisplayName("403 — ROLE_USER cannot delete")
        void regularUser_returns403() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(asUser()).with(csrf()))
                    .andExpect(status().isForbidden());
            verify(adminPartnerService, never()).deletePartner(any());
        }
    }
}