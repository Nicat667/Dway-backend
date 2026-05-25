package com.dway.dwaybackend.dto.request.challenge;

import com.dway.dwaybackend.entity.enums.Difficulty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateChallengeRequest {

    @NotBlank
    private String icon;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Difficulty difficulty;

    @NotNull
    @Min(1)
    private Integer targetCount;

    @NotNull
    @Min(0)
    private Integer rewardPoints;

    private Boolean isActive;

    @Future(message = "expiresAt must be a future date")
    private LocalDateTime expiresAt;
}