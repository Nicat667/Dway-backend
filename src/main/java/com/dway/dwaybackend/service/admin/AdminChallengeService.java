package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.challenge.ChallengeNotFoundException;
import com.dway.dwaybackend.dto.request.challenge.CreateChallengeRequest;
import com.dway.dwaybackend.dto.request.challenge.UpdateChallengeRequest;
import com.dway.dwaybackend.dto.response.challenge.AdminChallengeResponse;
import com.dway.dwaybackend.entity.Challenge;
import com.dway.dwaybackend.mapper.ChallengeMapper;
import com.dway.dwaybackend.repository.ChallengeRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeMapper challengeMapper;
    private final UserChallengeRepository userChallengeRepository;

    @Transactional(readOnly = true)
    public Page<AdminChallengeResponse> getAllChallenges(Boolean isActive, Pageable pageable) {
        Page<Challenge> page = (isActive == null)
                ? challengeRepository.findAll(pageable)
                : challengeRepository.findByIsActive(isActive, pageable);
        return page.map(challengeMapper::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public AdminChallengeResponse getChallengeById(UUID id) {
        return challengeRepository.findById(id).map(challengeMapper::toAdminResponse).orElseThrow(ChallengeNotFoundException::new);
    }

    @Transactional
    public AdminChallengeResponse createChallenge(CreateChallengeRequest request) {
        Challenge challenge = challengeMapper.toEntity(request);
        if (request.getIsActive() != null)  challenge.setActive(request.getIsActive());
        if (request.getExpiresAt() != null) challenge.setExpiresAt(request.getExpiresAt());
        challengeRepository.save(challenge);
        log.info("Admin created challenge id={} title={}", challenge.getId(), challenge.getTitle());
        return challengeMapper.toAdminResponse(challenge);
    }

    @Transactional
    public AdminChallengeResponse updateChallenge(UUID id, UpdateChallengeRequest request) {
        Challenge challenge = challengeRepository.findById(id).orElseThrow(ChallengeNotFoundException::new);

        if (request.getIcon() != null)         challenge.setIcon(request.getIcon());
        if (request.getTitle() != null)        challenge.setTitle(request.getTitle());
        if (request.getDescription() != null)  challenge.setDescription(request.getDescription());
        if (request.getDifficulty() != null)   challenge.setDifficulty(request.getDifficulty());
        if (request.getTargetCount() != null)  challenge.setTargetCount(request.getTargetCount());
        if (request.getRewardPoints() != null) challenge.setRewardPoints(request.getRewardPoints());
        if (request.getExpiresAt() != null)    challenge.setExpiresAt(request.getExpiresAt());

        challengeRepository.save(challenge);
        log.info("Admin updated challenge {}", id);
        return challengeMapper.toAdminResponse(challenge);
    }

    @Transactional
    public AdminChallengeResponse toggleActive(UUID id) {
        Challenge challenge = challengeRepository.findById(id).orElseThrow(ChallengeNotFoundException::new);
        challenge.setActive(!challenge.isActive());
        challengeRepository.save(challenge);
        log.info("Admin toggled challenge {} -> isActive={}", id, challenge.isActive());
        return challengeMapper.toAdminResponse(challenge);
    }

    @Transactional
    public void deleteChallenge(UUID id) {
        Challenge challenge = challengeRepository.findById(id).orElseThrow(ChallengeNotFoundException::new);
        userChallengeRepository.deleteAllByChallengeId(id);
        challengeRepository.delete(challenge);
        log.info("Admin deleted challenge {}", id);
    }
}