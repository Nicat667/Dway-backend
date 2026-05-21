package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.request.task.CreateTaskRequest;
import com.dway.dwaybackend.dto.response.task.TaskResponse;
import com.dway.dwaybackend.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    // Ignore auto-managed fields when mapping from request to entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "isCompleted", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(CreateTaskRequest request);

    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);
}