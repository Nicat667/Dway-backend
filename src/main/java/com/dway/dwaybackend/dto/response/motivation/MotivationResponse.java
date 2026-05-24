package com.dway.dwaybackend.dto.response.motivation;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MotivationResponse {

    private UUID id;
    private String quote;
    private String author;
    private LocalDate lastShownDate;   // null = never shown yet
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}