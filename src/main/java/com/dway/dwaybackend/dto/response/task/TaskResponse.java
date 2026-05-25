package com.dway.dwaybackend.dto.response.task;

import com.dway.dwaybackend.entity.enums.Period;
import com.dway.dwaybackend.entity.enums.Priority;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class TaskResponse {

    private UUID id;
    private UUID userId;
    private UUID categoryId;
    private String title;
    private Priority priority;
    private Period period;
    private LocalDateTime dueDate;
    private LocalDateTime alarmTime;
    private String notes;
    private Boolean isCompleted;   // Boolean wrapper — serializes as "isCompleted" not "completed"
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}