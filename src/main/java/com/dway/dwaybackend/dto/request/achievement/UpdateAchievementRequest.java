package com.dway.dwaybackend.dto.request.achievement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAchievementRequest {

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    private String description;

    private String icon;

    @Min(value = 1, message = "Threshold must be at least 1")
    private Integer threshold;
}