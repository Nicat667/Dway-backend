package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.achievement.AchievementNotFoundException;
import com.dway.dwaybackend.dto.request.achievement.CreateAchievementRequest;
import com.dway.dwaybackend.dto.request.achievement.UpdateAchievementRequest;
import com.dway.dwaybackend.dto.response.achievement.AchievementResponse;
import com.dway.dwaybackend.entity.Achievement;
import com.dway.dwaybackend.mapper.AchievementMapper;
import com.dway.dwaybackend.repository.AchievementRepository;
import com.dway.dwaybackend.repository.UserAchievementRepository;
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
public class AdminAchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementMapper achievementMapper;

    @Transactional(readOnly = true)
    public Page<AchievementResponse> getAllAchievements(Boolean isActive, Pageable pageable) {
        Page<Achievement> page = (isActive == null)
                ? achievementRepository.findAll(pageable)
                : achievementRepository.findByIsActive(isActive, pageable);
        return page.map(achievementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AchievementResponse getAchievementById(UUID id) {
        return achievementRepository.findById(id)
                .map(achievementMapper::toResponse)
                .orElseThrow(AchievementNotFoundException::new);
    }

    @Transactional
    public AchievementResponse createAchievement(CreateAchievementRequest request) {
        Achievement achievement = achievementMapper.toEntity(request);
        // isActive defaults to false — admin must explicitly activate via toggle
        achievementRepository.save(achievement);
        log.info("Admin created achievement id={} title={}", achievement.getId(), achievement.getTitle());
        return achievementMapper.toResponse(achievement);
    }

    @Transactional
    public AchievementResponse updateAchievement(UUID id, UpdateAchievementRequest request) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(AchievementNotFoundException::new);

        if (request.getTitle() != null)        achievement.setTitle(request.getTitle());
        if (request.getDescription() != null)  achievement.setDescription(request.getDescription());
        if (request.getIcon() != null)         achievement.setIcon(request.getIcon());
        if (request.getThreshold() != null)    achievement.setThreshold(request.getThreshold());
//        if (request.getRewardPoints() != null) achievement.setRewardPoints(request.getRewardPoints());

        achievementRepository.save(achievement);
        log.info("Admin updated achievement {}", id);
        return achievementMapper.toResponse(achievement);
    }

    @Transactional
    public AchievementResponse toggleActive(UUID id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(AchievementNotFoundException::new);
        achievement.setActive(!achievement.isActive());
        achievementRepository.save(achievement);
        log.info("Admin toggled achievement {} -> isActive={}", id, achievement.isActive());
        return achievementMapper.toResponse(achievement);
    }

    @Transactional
    public void deleteAchievement(UUID id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(AchievementNotFoundException::new);
        userAchievementRepository.deleteAllByAchievementId(id);
        achievementRepository.delete(achievement);
        log.info("Admin deleted achievement {}", id);
    }
}