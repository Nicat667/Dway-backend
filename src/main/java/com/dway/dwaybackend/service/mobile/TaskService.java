package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.task.TaskAlreadyCompletedException;
import com.dway.dwaybackend.common.exception.task.TaskNotFoundException;
import com.dway.dwaybackend.common.exception.task.TaskNotCompletedException;
import com.dway.dwaybackend.dto.request.task.CreateTaskRequest;
import com.dway.dwaybackend.dto.request.task.UpdateTaskRequest;
import com.dway.dwaybackend.dto.response.task.TaskResponse;
import com.dway.dwaybackend.entity.Task;
import com.dway.dwaybackend.entity.enums.Period;
import com.dway.dwaybackend.mapper.TaskMapper;
import com.dway.dwaybackend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ChallengeProgressService challengeProgressService;
    private final AchievementUnlockService achievementUnlockService;

    @Transactional
    public TaskResponse createTask(UUID userId, CreateTaskRequest request) {
        Task task = taskMapper.toEntity(request);
        task.setUserId(userId);
        taskRepository.save(task);
        log.info("User {} created task '{}'", userId, task.getTitle());
        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks(UUID userId, Period period, UUID categoryId) {
        List<Task> tasks;
        if (period != null) {
            tasks = taskRepository.findByUserIdAndPeriod(userId, period.name());
        } else if (categoryId != null) {
            tasks = taskRepository.findByUserIdAndCategoryId(userId, categoryId);
        } else {
            tasks = taskRepository.findAllActiveByUserId(userId);
        }
        return taskMapper.toResponseList(tasks);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID userId, UUID taskId) {
        return taskMapper.toResponse(findOwnedTask(userId, taskId));
    }

    @Transactional
    public TaskResponse updateTask(UUID userId, UUID taskId, UpdateTaskRequest request) {
        Task task = findOwnedTask(userId, taskId);

        if (request.getTitle() != null)     task.setTitle(request.getTitle());
        if (request.getPriority() != null)  task.setPriority(request.getPriority());
        if (request.getPeriod() != null)    task.setPeriod(request.getPeriod());
        if (request.getDueDate() != null)   task.setDueDate(request.getDueDate());
        if (request.getAlarmTime() != null) task.setAlarmTime(request.getAlarmTime());
        if (request.getNotes() != null)     task.setNotes(request.getNotes());
        task.setCategoryId(request.getCategoryId());

        taskRepository.save(task);
        log.info("User {} updated task {}", userId, taskId);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse completeTask(UUID userId, UUID taskId) {
        Task task = findOwnedTask(userId, taskId);

        if (task.isCompleted()) throw new TaskAlreadyCompletedException();

        task.setCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

        challengeProgressService.recalculateProgress(userId);
        achievementUnlockService.checkAndUnlock(userId);

        log.info("User {} completed task {}", userId, taskId);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse uncompleteTask(UUID userId, UUID taskId) {
        Task task = findOwnedTask(userId, taskId);

        if (!task.isCompleted()) throw new TaskNotCompletedException();

        task.setCompleted(false);
        task.setCompletedAt(null);
        taskRepository.save(task);

        challengeProgressService.recalculateProgress(userId);

        log.info("User {} uncompleted task {}", userId, taskId);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        Task task = findOwnedTask(userId, taskId);
        task.setDeleted(true);
        taskRepository.save(task);
        log.info("User {} soft-deleted task {}", userId, taskId);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> syncTasks(UUID userId, LocalDateTime since) {
        return taskMapper.toResponseList(taskRepository.findByUserIdUpdatedAfter(userId, since));
    }

    private Task findOwnedTask(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndUserIdAndIsDeletedFalse(taskId, userId).orElseThrow(TaskNotFoundException::new);
    }
}