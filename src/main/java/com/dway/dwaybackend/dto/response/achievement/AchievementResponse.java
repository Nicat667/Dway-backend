package com.dway.dwaybackend.dto.response.achievement;

import com.dway.dwaybackend.entity.enums.AchievementType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AchievementResponse {

    private UUID id;
    private String title;
    private String description;
    private String icon;
    private AchievementType type;
    private int threshold;
    private Boolean isActive;
    private LocalDateTime createdAt;
}