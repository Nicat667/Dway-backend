package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.request.achievement.CreateAchievementRequest;
import com.dway.dwaybackend.dto.response.achievement.AchievementResponse;
import com.dway.dwaybackend.dto.response.achievement.AchievementStatusResponse;
import com.dway.dwaybackend.entity.Achievement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface AchievementMapper {

    @Mapping(source = "active", target = "isActive")
    AchievementResponse toResponse(Achievement achievement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Achievement toEntity(CreateAchievementRequest request);

    default AchievementStatusResponse toStatusResponse(Achievement achievement, boolean earned, LocalDateTime unlockedAt) {
        return AchievementStatusResponse.builder()
                .id(achievement.getId())
                .title(achievement.getTitle())
                .description(achievement.getDescription())
                .icon(achievement.getIcon())
                .type(achievement.getType())
                .threshold(achievement.getThreshold())
                .createdAt(achievement.getCreatedAt())
                .earned(earned)
                .unlockedAt(unlockedAt)
                .build();
    }
}