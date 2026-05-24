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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPartnerService {

    private static final String S3_PREFIX = "partners";

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;
    private final S3StorageService s3StorageService;


    @Transactional(readOnly = true)
    public Page<PartnerResponse> getAllPartners(Boolean isActive, Pageable pageable) {
        Page<Partner> page = (isActive == null)
                ? partnerRepository.findAllByOrderByCreatedAtDesc(pageable)
                : partnerRepository.findByIsActiveOrderByCreatedAtDesc(isActive, pageable);
        return page.map(partnerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PartnerResponse getPartnerById(UUID id) {
        return partnerRepository.findById(id)
                .map(partnerMapper::toResponse)
                .orElseThrow(PartnerNotFoundException::new);
    }

    @CacheEvict(value = "partners", allEntries = true)
    @Transactional
    public PartnerResponse createPartner(CreatePartnerRequest request, MultipartFile file) {
        if (partnerRepository.existsByName(request.getName())) {
            throw new PartnerNameExistsException();
        }

        String iconUrl = s3StorageService.upload(file, S3_PREFIX);

        Partner partner = partnerMapper.toEntity(request);
        partner.setIconUrl(iconUrl);
        if (request.getIsActive() != null) {
            partner.setActive(request.getIsActive());
        }

        partnerRepository.save(partner);
        log.info("Admin created partner id={} name={}", partner.getId(), partner.getName());
        return partnerMapper.toResponse(partner);
    }

    @CacheEvict(value = "partners", allEntries = true)
    @Transactional
    public PartnerResponse updatePartner(UUID id, UpdatePartnerRequest request) {
        Partner partner = partnerRepository.findById(id).orElseThrow(PartnerNotFoundException::new);

        if (request.getName() != null && !request.getName().equals(partner.getName())) {
            if (partnerRepository.existsByName(request.getName())) {
                throw new PartnerNameExistsException();
            }
            partner.setName(request.getName());
        }
        if (request.getDescription() != null) {
            partner.setDescription(request.getDescription());
        }
        if (request.getDiscountText() != null) {
            partner.setDiscountText(request.getDiscountText());
        }
        if (request.getPromoCode() != null) {
            partner.setPromoCode(request.getPromoCode());
        }
        if (request.getPartnerUrl() != null) {
            partner.setPartnerUrl(request.getPartnerUrl());
        }
        if (request.getIsActive() != null) {
            partner.setActive(request.getIsActive());
        }

        partnerRepository.save(partner);
        log.info("Admin updated partner {}", id);
        return partnerMapper.toResponse(partner);
    }

    @CacheEvict(value = "partners", allEntries = true)
    @Transactional
    public PartnerResponse updateIcon(UUID id, MultipartFile file) {
        Partner partner = partnerRepository.findById(id).orElseThrow(PartnerNotFoundException::new);

        s3StorageService.delete(partner.getIconUrl());

        String newIconUrl = s3StorageService.upload(file, S3_PREFIX);
        partner.setIconUrl(newIconUrl);
        partnerRepository.save(partner);

        log.info("Admin replaced icon for partner {}", id);
        return partnerMapper.toResponse(partner);
    }

    @CacheEvict(value = "partners", allEntries = true)
    @Transactional
    public void deletePartner(UUID id) {
        Partner partner = partnerRepository.findById(id).orElseThrow(PartnerNotFoundException::new);

        s3StorageService.delete(partner.getIconUrl());
        partnerRepository.delete(partner);

        log.info("Admin deleted partner {}", id);
    }
}