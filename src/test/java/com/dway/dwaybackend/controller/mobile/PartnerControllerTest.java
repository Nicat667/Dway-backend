package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.mobile.PartnerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PartnerController.class)
@Import(SecurityConfig.class)
@DisplayName("PartnerController")
class PartnerControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PartnerService partnerService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final String BASE = "/api/v1/mobile/partners";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private PartnerResponse stub(String name) {
        return PartnerResponse.builder()
                .id(UUID.randomUUID())
                .name(name)
                .iconUrl("https://bucket.s3.amazonaws.com/partners/icon.png")
                .description("desc")
                .discountText("20% off")
                .promoCode("CODE20")
                .partnerUrl("https://partner.az")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("GET / — getActivePartners")
    class GetActivePartners {

        @Test
        @DisplayName("200 — returns list of active partners with all fields")
        void success_returnsList() throws Exception {
            when(partnerService.getActivePartners())
                    .thenReturn(List.of(stub("Bravo"), stub("Kapital")));

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("Bravo"))
                    .andExpect(jsonPath("$.data[1].name").value("Kapital"))
                    .andExpect(jsonPath("$.data[0].isActive").value(true));
        }

        @Test
        @DisplayName("200 — returns empty array when no active partners")
        void success_emptyList() throws Exception {
            when(partnerService.getActivePartners()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("200 — iconUrl and promoCode are present in each item")
        void success_fieldsPresent() throws Exception {
            when(partnerService.getActivePartners()).thenReturn(List.of(stub("Bravo")));

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].iconUrl").isNotEmpty())
                    .andExpect(jsonPath("$.data[0].promoCode").value("CODE20"))
                    .andExpect(jsonPath("$.data[0].partnerUrl").value("https://partner.az"));
        }

        @Test
        @DisplayName("200 — service called exactly once")
        void success_serviceCalledOnce() throws Exception {
            when(partnerService.getActivePartners()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE).with(asUser()));

            verify(partnerService, times(1)).getActivePartners();
        }

        @Test
        @DisplayName("401 — unauthenticated request")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
            verify(partnerService, never()).getActivePartners();
        }
    }
}