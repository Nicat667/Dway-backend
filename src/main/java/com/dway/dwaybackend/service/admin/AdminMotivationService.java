package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.motivation.MotivationNotFoundException;
import com.dway.dwaybackend.dto.request.motivation.CreateMotivationRequest;
import com.dway.dwaybackend.dto.request.motivation.UpdateMotivationRequest;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.entity.DailyMotivation;
import com.dway.dwaybackend.mapper.MotivationMapper;
import com.dway.dwaybackend.repository.DailyMotivationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMotivationService {

    private final DailyMotivationRepository motivationRepository;
    private final MotivationMapper motivationMapper;

    @Transactional(readOnly = true)
    public Page<MotivationResponse> getAllMotivations(Pageable pageable) {
        return motivationRepository.findAllByOrderByLastShownDateAscCreatedAtAsc(pageable)
                .map(motivationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MotivationResponse getMotivationById(UUID id) {
        return motivationRepository.findById(id)
                .map(motivationMapper::toResponse)
                .orElseThrow(MotivationNotFoundException::new);
    }

    // No cache evict on create — the new motivation joins the back of the queue
    // via lastShownDate = today and does not affect what is shown today.
    @Transactional
    public MotivationResponse createMotivation(CreateMotivationRequest request) {
        DailyMotivation motivation = motivationMapper.toEntity(request);
        // Set lastShownDate to today so the new entry goes to the back of the queue.
        // If left null, it would jump to the front (nulls come first in the queue order).
        motivation.setLastShownDate(LocalDate.now());
        motivationRepository.save(motivation);
        log.info("Admin uploaded motivation id={}", motivation.getId());
        return motivationMapper.toResponse(motivation);
    }

    @CacheEvict(value = "motivations", allEntries = true)
    @Transactional
    public MotivationResponse updateMotivation(UUID id, UpdateMotivationRequest request) {
        DailyMotivation motivation = motivationRepository.findById(id).orElseThrow(MotivationNotFoundException::new);

        if (request.getQuote() != null) {
            motivation.setQuote(request.getQuote());
        }
        if (request.getAuthor() != null) {
            motivation.setAuthor(request.getAuthor());
        }

        motivationRepository.save(motivation);
        log.info("Admin updated motivation {}", id);
        return motivationMapper.toResponse(motivation);
    }

    @CacheEvict(value = "motivations", allEntries = true)
    @Transactional
    public void deleteMotivation(UUID id) {
        DailyMotivation motivation = motivationRepository.findById(id)
                .orElseThrow(MotivationNotFoundException::new);
        motivationRepository.delete(motivation);
        log.info("Admin deleted motivation {}", id);
    }
}