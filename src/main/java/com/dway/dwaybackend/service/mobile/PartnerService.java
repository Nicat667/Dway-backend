package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.mapper.PartnerMapper;
import com.dway.dwaybackend.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;

    // Cache key = 'active'. TTL = 1h (configured in RedisConfig).
    // Evicted on any admin create/update/delete via @CacheEvict(allEntries = true).
    @Cacheable(value = "partners", key = "'active'")
    @Transactional(readOnly = true)
    public List<PartnerResponse> getActivePartners() {
        log.debug("Cache miss — loading active partners from DB");
        return partnerRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(partnerMapper::toResponse)
                .toList();
    }
}