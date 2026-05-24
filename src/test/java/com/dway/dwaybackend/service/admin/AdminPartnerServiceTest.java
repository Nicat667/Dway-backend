package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.partner.PartnerNameExistsException;
import com.dway.dwaybackend.common.exception.partner.PartnerNotFoundException;
import com.dway.dwaybackend.dto.request.partner.CreatePartnerRequest;
import com.dway.dwaybackend.dto.request.partner.UpdatePartnerRequest;
import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.entity.Partner;
import com.dway.dwaybackend.infrastructure.storage.S3StorageService;
import com.dway.dwaybackend.mapper.PartnerMapper;
import com.dway.dwaybackend.repository.PartnerRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPartnerService")
class AdminPartnerServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerMapper partnerMapper;
    @Mock private S3StorageService s3StorageService;

    @InjectMocks private AdminPartnerService adminPartnerService;

    private static final UUID   ID       = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String ICON_URL = "https://bucket.s3.eu-north-1.amazonaws.com/partners/icon.png";

    private Partner entity() {
        return Partner.builder()
                .id(ID)
                .name("Bravo")
                .iconUrl(ICON_URL)
                .description("Big discount")
                .discountText("20% off")
                .promoCode("BRAVO20")
                .partnerUrl("https://bravo.az")
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
                .description(p.getDescription())
                .discountText(p.getDiscountText())
                .promoCode(p.getPromoCode())
                .partnerUrl(p.getPartnerUrl())
                .isActive(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private CreatePartnerRequest createReq() {
        CreatePartnerRequest r = new CreatePartnerRequest();
        r.setName("Bravo");
        r.setDescription("Big discount");
        r.setDiscountText("20% off");
        r.setPromoCode("BRAVO20");
        r.setPartnerUrl("https://bravo.az");
        return r;
    }

    private MultipartFile mockFile() {
        return new MockMultipartFile("file", "icon.png", "image/png", new byte[]{1, 2, 3});
    }

    @Nested
    @DisplayName("getAllPartners()")
    class GetAllPartners {

        @Test
        @DisplayName("null isActive → calls findAllByOrderByCreatedAtDesc")
        void nullIsActive_callsAll() {
            Pageable pageable = PageRequest.of(0, 20);
            when(partnerRepository.findAllByOrderByCreatedAtDesc(pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            adminPartnerService.getAllPartners(null, pageable);

            verify(partnerRepository).findAllByOrderByCreatedAtDesc(pageable);
            verify(partnerRepository, never()).findByIsActiveOrderByCreatedAtDesc(anyBoolean(), any());
        }

        @Test
        @DisplayName("isActive=true → calls findByIsActiveOrderByCreatedAtDesc with true")
        void isActiveTrue_callsFiltered() {
            Pageable pageable = PageRequest.of(0, 20);
            when(partnerRepository.findByIsActiveOrderByCreatedAtDesc(true, pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            adminPartnerService.getAllPartners(true, pageable);

            verify(partnerRepository).findByIsActiveOrderByCreatedAtDesc(true, pageable);
            verify(partnerRepository, never()).findAllByOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("isActive=false → calls findByIsActiveOrderByCreatedAtDesc with false")
        void isActiveFalse_callsFiltered() {
            Pageable pageable = PageRequest.of(0, 20);
            when(partnerRepository.findByIsActiveOrderByCreatedAtDesc(false, pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            adminPartnerService.getAllPartners(false, pageable);

            verify(partnerRepository).findByIsActiveOrderByCreatedAtDesc(false, pageable);
        }

        @Test
        @DisplayName("returns mapped page of responses")
        void returnsMappedPage() {
            Partner p = entity();
            Pageable pageable = PageRequest.of(0, 20);
            when(partnerRepository.findAllByOrderByCreatedAtDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(p)));
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            Page<PartnerResponse> result = adminPartnerService.getAllPartners(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("returns empty page when no partners exist")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(partnerRepository.findAllByOrderByCreatedAtDesc(pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<PartnerResponse> result = adminPartnerService.getAllPartners(null, pageable);

            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getPartnerById()")
    class GetPartnerById {

        @Test
        @DisplayName("returns mapped response when partner is found")
        void returnsResponseWhenFound() {
            Partner p = entity();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            PartnerResponse result = adminPartnerService.getPartnerById(ID);

            assertThat(result.getId()).isEqualTo(ID);
            assertThat(result.getName()).isEqualTo("Bravo");
        }

        @Test
        @DisplayName("throws PartnerNotFoundException when partner does not exist")
        void throwsWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.getPartnerById(ID));
        }

        @Test
        @DisplayName("does not call mapper when partner is not found")
        void doesNotMapWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.getPartnerById(ID));

            verify(partnerMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("createPartner()")
    class CreatePartner {

        @Test
        @DisplayName("saves partner and returns mapped response")
        void savesAndReturnsResponse() {
            CreatePartnerRequest req = createReq();
            Partner p = entity();
            when(partnerRepository.existsByName("Bravo")).thenReturn(false);
            when(s3StorageService.upload(any(), eq("partners"))).thenReturn(ICON_URL);
            when(partnerMapper.toEntity(req)).thenReturn(p);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            PartnerResponse result = adminPartnerService.createPartner(req, mockFile());

            assertThat(result.getId()).isEqualTo(ID);
            verify(partnerRepository).save(p);
        }

        @Test
        @DisplayName("throws PartnerNameExistsException when name is already taken")
        void throwsWhenNameExists() {
            CreatePartnerRequest req = createReq();
            when(partnerRepository.existsByName("Bravo")).thenReturn(true);

            assertThrows(PartnerNameExistsException.class,
                    () -> adminPartnerService.createPartner(req, mockFile()));
        }

        @Test
        @DisplayName("name check is done BEFORE S3 upload — no orphaned objects on duplicate name")
        void nameCheckBeforeS3Upload() {
            CreatePartnerRequest req = createReq();
            when(partnerRepository.existsByName("Bravo")).thenReturn(true);

            assertThrows(PartnerNameExistsException.class,
                    () -> adminPartnerService.createPartner(req, mockFile()));

            verify(s3StorageService, never()).upload(any(), any());
        }

        @Test
        @DisplayName("sets iconUrl from S3 upload result onto the entity before save")
        void setsIconUrlBeforeSave() {
            CreatePartnerRequest req = createReq();
            Partner p = entity();
            p.setIconUrl(null); // mapper returns entity without iconUrl
            when(partnerRepository.existsByName("Bravo")).thenReturn(false);
            when(s3StorageService.upload(any(), eq("partners"))).thenReturn(ICON_URL);
            when(partnerMapper.toEntity(req)).thenReturn(p);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            doAnswer(inv -> {
                Partner saved = inv.getArgument(0);
                assertThat(saved.getIconUrl()).isEqualTo(ICON_URL);
                return null;
            }).when(partnerRepository).save(p);

            adminPartnerService.createPartner(req, mockFile());
        }

        @Test
        @DisplayName("applies isActive from request when explicitly set")
        void appliesIsActiveFromRequest() {
            CreatePartnerRequest req = createReq();
            req.setIsActive(false);
            Partner p = entity();
            when(partnerRepository.existsByName("Bravo")).thenReturn(false);
            when(s3StorageService.upload(any(), any())).thenReturn(ICON_URL);
            when(partnerMapper.toEntity(req)).thenReturn(p);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.createPartner(req, mockFile());

            assertThat(p.isActive()).isFalse();
        }

        @Test
        @DisplayName("does not call save when name exists")
        void doesNotSaveWhenNameExists() {
            when(partnerRepository.existsByName("Bravo")).thenReturn(true);

            assertThrows(PartnerNameExistsException.class,
                    () -> adminPartnerService.createPartner(createReq(), mockFile()));

            verify(partnerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePartner()")
    class UpdatePartner {

        private UpdatePartnerRequest fullReq() {
            UpdatePartnerRequest r = new UpdatePartnerRequest();
            r.setName("Kapital");
            r.setDescription("Bank partner");
            r.setDiscountText("10% cashback");
            r.setPromoCode("KAP10");
            r.setPartnerUrl("https://kapital.az");
            r.setIsActive(false);
            return r;
        }

        @Test
        @DisplayName("updates all provided fields")
        void updatesAllFields() {
            Partner p = entity();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerRepository.existsByName("Kapital")).thenReturn(false);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updatePartner(ID, fullReq());

            assertThat(p.getName()).isEqualTo("Kapital");
            assertThat(p.getDescription()).isEqualTo("Bank partner");
            assertThat(p.getDiscountText()).isEqualTo("10% cashback");
            assertThat(p.getPromoCode()).isEqualTo("KAP10");
            assertThat(p.getPartnerUrl()).isEqualTo("https://kapital.az");
            assertThat(p.isActive()).isFalse();
        }

        @Test
        @DisplayName("null fields are skipped — PATCH semantics")
        void nullFieldsSkipped() {
            Partner p = entity();
            String originalName = p.getName();
            String originalDesc = p.getDescription();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updatePartner(ID, new UpdatePartnerRequest()); // all null

            assertThat(p.getName()).isEqualTo(originalName);
            assertThat(p.getDescription()).isEqualTo(originalDesc);
        }

        @Test
        @DisplayName("name change with unique new name is allowed")
        void nameChangeAllowedWhenUnique() {
            Partner p = entity();
            UpdatePartnerRequest req = new UpdatePartnerRequest();
            req.setName("NewName");
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerRepository.existsByName("NewName")).thenReturn(false);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updatePartner(ID, req);

            assertThat(p.getName()).isEqualTo("NewName");
        }

        @Test
        @DisplayName("setting the same name does not trigger uniqueness check")
        void sameNameSkipsUniquenessCheck() {
            Partner p = entity(); // name = "Bravo"
            UpdatePartnerRequest req = new UpdatePartnerRequest();
            req.setName("Bravo");
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updatePartner(ID, req);

            verify(partnerRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("throws PartnerNameExistsException when new name is already taken")
        void throwsWhenNewNameTaken() {
            Partner p = entity();
            UpdatePartnerRequest req = new UpdatePartnerRequest();
            req.setName("Kapital");
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerRepository.existsByName("Kapital")).thenReturn(true);

            assertThrows(PartnerNameExistsException.class,
                    () -> adminPartnerService.updatePartner(ID, req));
        }

        @Test
        @DisplayName("throws PartnerNotFoundException when partner does not exist")
        void throwsWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.updatePartner(ID, new UpdatePartnerRequest()));
        }

        @Test
        @DisplayName("saves the entity after applying changes")
        void savesAfterUpdate() {
            Partner p = entity();
            UpdatePartnerRequest req = new UpdatePartnerRequest();
            req.setDescription("Updated desc");
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updatePartner(ID, req);

            verify(partnerRepository).save(p);
        }

        @Test
        @DisplayName("does not touch iconUrl — icon changes go through updateIcon endpoint")
        void doesNotTouchIconUrl() {
            Partner p = entity();
            String originalIconUrl = p.getIconUrl();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updatePartner(ID, fullReq());

            assertThat(p.getIconUrl()).isEqualTo(originalIconUrl);
            verify(s3StorageService, never()).upload(any(), any());
        }
    }

    @Nested
    @DisplayName("updateIcon()")
    class UpdateIcon {

        @Test
        @DisplayName("deletes old icon then uploads new one")
        void deletesOldThenUploadsNew() {
            Partner p = entity();
            String newUrl = "https://bucket.s3.eu-north-1.amazonaws.com/partners/new.png";
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(s3StorageService.upload(any(), eq("partners"))).thenReturn(newUrl);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            InOrder order = inOrder(s3StorageService, partnerRepository);

            adminPartnerService.updateIcon(ID, mockFile());

            order.verify(s3StorageService).delete(ICON_URL);
            order.verify(s3StorageService).upload(any(), eq("partners"));
            order.verify(partnerRepository).save(p);
        }

        @Test
        @DisplayName("sets new iconUrl on entity before save")
        void setsNewIconUrl() {
            String newUrl = "https://bucket.s3.eu-north-1.amazonaws.com/partners/new.png";
            Partner p = entity();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));
            when(s3StorageService.upload(any(), any())).thenReturn(newUrl);
            when(partnerMapper.toResponse(p)).thenReturn(response(p));

            adminPartnerService.updateIcon(ID, mockFile());

            assertThat(p.getIconUrl()).isEqualTo(newUrl);
        }

        @Test
        @DisplayName("throws PartnerNotFoundException when partner does not exist")
        void throwsWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.updateIcon(ID, mockFile()));
        }

        @Test
        @DisplayName("does not upload when partner not found")
        void doesNotUploadWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.updateIcon(ID, mockFile()));

            verify(s3StorageService, never()).upload(any(), any());
        }
    }

    @Nested
    @DisplayName("deletePartner()")
    class DeletePartner {

        @Test
        @DisplayName("deletes icon from S3 then removes partner from DB")
        void deletesS3ThenDB() {
            Partner p = entity();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));

            InOrder order = inOrder(s3StorageService, partnerRepository);

            adminPartnerService.deletePartner(ID);

            order.verify(s3StorageService).delete(ICON_URL);
            order.verify(partnerRepository).delete(p);
        }

        @Test
        @DisplayName("throws PartnerNotFoundException when partner does not exist")
        void throwsWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.deletePartner(ID));
        }

        @Test
        @DisplayName("does not call S3 delete when partner not found")
        void doesNotDeleteS3WhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.deletePartner(ID));

            verify(s3StorageService, never()).delete(any());
        }

        @Test
        @DisplayName("does not call repository delete when partner not found")
        void doesNotDeleteDBWhenNotFound() {
            when(partnerRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(PartnerNotFoundException.class,
                    () -> adminPartnerService.deletePartner(ID));

            verify(partnerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("never calls save — physical delete only")
        void neverCallsSave() {
            Partner p = entity();
            when(partnerRepository.findById(ID)).thenReturn(Optional.of(p));

            adminPartnerService.deletePartner(ID);

            verify(partnerRepository, never()).save(any());
        }
    }
}