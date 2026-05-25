package com.dway.dwaybackend.dto.response.challenge;

import com.dway.dwaybackend.entity.enums.Difficulty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AdminChallengeResponse {

    private UUID id;
    private String icon;
    private String title;
    private String description;
    private Difficulty difficulty;
    private int targetCount;
    private int rewardPoints;
    private Boolean isActive;
    private int participantCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}