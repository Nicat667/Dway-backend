package com.dway.dwaybackend.dto.request.challenge;

import com.dway.dwaybackend.entity.enums.Difficulty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateChallengeRequest {

    private String icon;
    private String title;
    private String description;
    private Difficulty difficulty;

    @Min(1)
    private Integer targetCount;

    @Min(0)
    private Integer rewardPoints;

    @Future(message = "expiresAt must be a future date")
    private LocalDateTime expiresAt;
}