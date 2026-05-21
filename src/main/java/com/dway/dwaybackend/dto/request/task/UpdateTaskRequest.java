package com.dway.dwaybackend.dto.request.task;

import com.dway.dwaybackend.entity.enums.Period;
import com.dway.dwaybackend.entity.enums.Priority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UpdateTaskRequest {

    // All fields are optional — only non-null values are applied (PATCH semantics)

    private String title;

    private Priority priority;

    private UUID categoryId;

    private Period period;

    private LocalDateTime dueDate;

    private LocalDateTime alarmTime;

    private String notes;
}