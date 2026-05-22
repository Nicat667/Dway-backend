package com.dway.dwaybackend.dto.response.category;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class CategoryResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String icon;
    private String color;
    private boolean isDefault;
    private LocalDateTime createdAt;
}