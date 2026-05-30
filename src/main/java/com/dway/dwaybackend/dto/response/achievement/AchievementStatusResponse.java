package com.dway.dwaybackend.dto.response.achievement;

import com.dway.dwaybackend.entity.enums.AchievementType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AchievementStatusResponse {

    private UUID id;
    private String title;
    private String description;
    private String icon;
    private AchievementType type;
    private int threshold;
    private LocalDateTime createdAt;

    private boolean earned;
    private LocalDateTime unlockedAt;
}