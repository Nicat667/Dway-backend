package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.entity.Partner;
import com.dway.dwaybackend.mapper.PartnerMapper;
import com.dway.dwaybackend.repository.PartnerRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartnerService")
class PartnerServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerMapper partnerMapper;

    @InjectMocks private PartnerService partnerService;

    private Partner entity(String name) {
        return Partner.builder()
                .id(UUID.randomUUID())
                .name(name)
                .iconUrl("https://bucket.s3.amazonaws.com/partners/icon.png")
                .description("desc")
                .discountText("10% off")
                .promoCode("CODE")
                .partnerUrl("https://partner.az")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PartnerResponse response(Partner p) {
        return PartnerResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .iconUrl(p.getIconUrl())
                .isActive(p.isActive())
                .build();
    }

    @Nested
    @DisplayName("getActivePartners()")
    class GetActivePartners {

        @Test
        @DisplayName("calls findByIsActiveTrueOrderByCreatedAtDesc")
        void callsCorrectRepositoryMethod() {
            when(partnerRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(Collections.emptyList());

            partnerService.getActivePartners();

            verify(partnerRepository).findByIsActiveTrueOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("returns mapped list of active partners")
        void returnsMappedList() {
            Partner p1 = entity("Bravo");
            Partner p2 = entity("Kapital");
            when(partnerRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(List.of(p1, p2));
            when(partnerMapper.toResponse(p1)).thenReturn(response(p1));
            when(partnerMapper.toResponse(p2)).thenReturn(response(p2));

            List<PartnerResponse> result = partnerService.getActivePartners();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Bravo");
            assertThat(result.get(1).getName()).isEqualTo("Kapital");
        }

        @Test
        @DisplayName("returns empty list when no active partners exist")
        void returnsEmptyListWhenNoneActive() {
            when(partnerRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(Collections.emptyList());

            List<PartnerResponse> result = partnerService.getActivePartners();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("maps each entity through PartnerMapper")
        void mapsEachEntityThroughMapper() {
            Partner p1 = entity("A");
            Partner p2 = entity("B");
            Partner p3 = entity("C");
            when(partnerRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(List.of(p1, p2, p3));
            when(partnerMapper.toResponse(any())).thenReturn(response(p1));

            partnerService.getActivePartners();

            verify(partnerMapper, times(3)).toResponse(any());
        }

        @Test
        @DisplayName("does not call any other repository method")
        void callsOnlyActiveQuery() {
            when(partnerRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(Collections.emptyList());

            partnerService.getActivePartners();

            verify(partnerRepository).findByIsActiveTrueOrderByCreatedAtDesc();
            verify(partnerRepository, never()).findAllByOrderByCreatedAtDesc(any());
            verify(partnerRepository, never()).findByIsActiveOrderByCreatedAtDesc(anyBoolean(), any());
        }
    }
}