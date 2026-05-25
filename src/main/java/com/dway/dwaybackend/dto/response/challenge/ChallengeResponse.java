package com.dway.dwaybackend.dto.response.challenge;

import com.dway.dwaybackend.entity.enums.Difficulty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ChallengeResponse {

    private UUID id;
    private String icon;
    private String title;
    private String description;
    private Difficulty difficulty;
    private int targetCount;
    private int rewardPoints;
    private int participantCount;
    private LocalDateTime expiresAt;
    private Boolean isJoined;
    private int progress;
    private LocalDateTime completedAt;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
}