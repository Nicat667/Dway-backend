package com.dway.dwaybackend.dto.request.achievement;

import com.dway.dwaybackend.entity.enums.AchievementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateAchievementRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Icon is required")
    private String icon;

    @NotNull(message = "Type is required (TASK_COUNT or CHALLENGE_COUNT)")
    private AchievementType type;

    @Min(value = 1, message = "Threshold must be at least 1")
    private int threshold;
}