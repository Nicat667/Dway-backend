package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.task.CreateTaskRequest;
import com.dway.dwaybackend.dto.request.task.UpdateTaskRequest;
import com.dway.dwaybackend.dto.response.task.TaskResponse;
import com.dway.dwaybackend.entity.enums.Period;
import com.dway.dwaybackend.security.CurrentUser;
import com.dway.dwaybackend.service.mobile.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Tasks")
@RestController
@RequestMapping("/api/v1/mobile/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a new task")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@CurrentUser UUID userId, @RequestBody @Valid CreateTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task created", taskService.createTask(userId, request)));
    }

    @Operation(summary = "Get all active tasks. Optionally filter by period or categoryId (mutually exclusive; period takes priority)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks(@CurrentUser UUID userId, @RequestParam(required = false) Period period, @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success(null, taskService.getAllTasks(userId, period, categoryId)));
    }

    @Operation(summary = "Sync tasks updated after a given timestamp (for offline-first mobile sync)")
    @GetMapping("/sync")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> syncTasks(@CurrentUser UUID userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(ApiResponse.success(null, taskService.syncTasks(userId, since)));
    }

    @Operation(summary = "Get a single task by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(null, taskService.getTaskById(userId, id)));
    }

    @Operation(summary = "Update task fields (all optional, PATCH semantics). Sending categoryId as null removes the category")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task updated", taskService.updateTask(userId, id, request)));
    }

    @Operation(summary = "Mark task as completed")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Task completed", taskService.completeTask(userId, id)));
    }

    @Operation(summary = "Mark task as not completed")
    @PostMapping("/{id}/uncomplete")
    public ResponseEntity<ApiResponse<TaskResponse>> uncompleteTask(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Task uncompleted", taskService.uncompleteTask(userId, id)));
    }

    @Operation(summary = "Delete a task (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@CurrentUser UUID userId, @PathVariable UUID id) {
        taskService.deleteTask(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
    }
}