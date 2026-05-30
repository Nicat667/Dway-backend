package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.service.mobile.AchievementUnlockService;
import com.dway.dwaybackend.common.exception.task.TaskAlreadyCompletedException;
import com.dway.dwaybackend.common.exception.task.TaskNotFoundException;
import com.dway.dwaybackend.common.exception.task.TaskNotCompletedException;
import com.dway.dwaybackend.dto.request.task.CreateTaskRequest;
import com.dway.dwaybackend.dto.request.task.UpdateTaskRequest;
import com.dway.dwaybackend.dto.response.task.TaskResponse;
import com.dway.dwaybackend.entity.Task;
import com.dway.dwaybackend.entity.enums.Period;
import com.dway.dwaybackend.entity.enums.Priority;
import com.dway.dwaybackend.mapper.TaskMapper;
import com.dway.dwaybackend.repository.TaskRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private ChallengeProgressService challengeProgressService;
    @Mock private AchievementUnlockService achievementUnlockService;

    @InjectMocks private TaskService taskService;

    private static final UUID USER_ID     = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID TASK_ID     = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CATEGORY_ID = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");

    private Task task() {
        return Task.builder()
                .id(TASK_ID).userId(USER_ID).categoryId(CATEGORY_ID)
                .title("Finish report").priority(Priority.HIGH).period(Period.DAILY)
                .isCompleted(false).isDeleted(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private TaskResponse taskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId()).userId(task.getUserId()).categoryId(task.getCategoryId())
                .title(task.getTitle()).priority(task.getPriority()).period(task.getPeriod())
                .isCompleted(task.isCompleted()).completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt())
                .build();
    }

    private CreateTaskRequest createRequest() {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle("Finish report"); r.setPriority(Priority.HIGH);
        r.setCategoryId(CATEGORY_ID); r.setPeriod(Period.DAILY);
        return r;
    }

    private UpdateTaskRequest updateRequest() {
        UpdateTaskRequest r = new UpdateTaskRequest();
        r.setTitle("Updated title"); r.setPriority(Priority.LOW); r.setPeriod(Period.WEEKLY);
        r.setDueDate(LocalDateTime.now().plusDays(3)); r.setAlarmTime(LocalDateTime.now().plusDays(2));
        r.setNotes("Some notes"); r.setCategoryId(CATEGORY_ID);
        return r;
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Nested @DisplayName("createTask()")
    class CreateTask {

        @Test @DisplayName("saves task with correct userId and returns response")
        void withValidRequest_savesAndReturnsResponse() {
            Task entity = task();
            when(taskMapper.toEntity(any())).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            TaskResponse result = taskService.createTask(USER_ID, createRequest());

            assertThat(result.getId()).isEqualTo(TASK_ID);
            verify(taskRepository).save(entity);
        }

        @Test @DisplayName("sets userId from parameter, not from request")
        void always_setsUserIdFromParameter() {
            Task entity = task();
            entity.setUserId(null);
            when(taskMapper.toEntity(any())).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.createTask(USER_ID, createRequest());

            assertThat(entity.getUserId()).isEqualTo(USER_ID);
        }

        @Test @DisplayName("persists task before returning response")
        void always_savesBeforeReturningResponse() {
            Task entity = task();
            when(taskMapper.toEntity(any())).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.createTask(USER_ID, createRequest());

            InOrder order = inOrder(taskRepository, taskMapper);
            order.verify(taskRepository).save(entity);
            order.verify(taskMapper).toResponse(entity);
        }
    }

    // ── getAllTasks ───────────────────────────────────────────────────────────

    @Nested @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test @DisplayName("returns all active tasks when no filters provided")
        void withNoFilters_returnsAllActiveTasks() {
            Task entity = task();
            when(taskRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of(entity));
            when(taskMapper.toResponseList(any())).thenReturn(List.of(taskResponse(entity)));

            List<TaskResponse> result = taskService.getAllTasks(USER_ID, null, null);

            assertThat(result).hasSize(1);
            verify(taskRepository).findAllActiveByUserId(USER_ID);
            verify(taskRepository, never()).findByUserIdAndPeriod(any(), any());
        }

        @Test @DisplayName("filters by period when period is provided")
        void withPeriod_callsFindByUserIdAndPeriod() {
            when(taskRepository.findByUserIdAndPeriod(USER_ID, "DAILY")).thenReturn(List.of(task()));
            when(taskMapper.toResponseList(any())).thenReturn(List.of());

            taskService.getAllTasks(USER_ID, Period.DAILY, null);

            verify(taskRepository).findByUserIdAndPeriod(USER_ID, "DAILY");
            verify(taskRepository, never()).findAllActiveByUserId(any());
        }

        @Test @DisplayName("filters by categoryId when provided")
        void withCategoryId_callsFindByUserIdAndCategoryId() {
            when(taskRepository.findByUserIdAndCategoryId(USER_ID, CATEGORY_ID)).thenReturn(List.of(task()));
            when(taskMapper.toResponseList(any())).thenReturn(List.of());

            taskService.getAllTasks(USER_ID, null, CATEGORY_ID);

            verify(taskRepository).findByUserIdAndCategoryId(USER_ID, CATEGORY_ID);
            verify(taskRepository, never()).findAllActiveByUserId(any());
        }

        @Test @DisplayName("period takes precedence over categoryId when both provided")
        void withBothFilters_periodTakesPrecedence() {
            when(taskRepository.findByUserIdAndPeriod(USER_ID, "WEEKLY")).thenReturn(List.of());
            when(taskMapper.toResponseList(any())).thenReturn(List.of());

            taskService.getAllTasks(USER_ID, Period.WEEKLY, CATEGORY_ID);

            verify(taskRepository).findByUserIdAndPeriod(USER_ID, "WEEKLY");
            verify(taskRepository, never()).findByUserIdAndCategoryId(any(), any());
        }

        @Test @DisplayName("returns empty list when user has no tasks")
        void whenNoTasksExist_returnsEmptyList() {
            when(taskRepository.findAllActiveByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(taskMapper.toResponseList(any())).thenReturn(Collections.emptyList());

            assertThat(taskService.getAllTasks(USER_ID, null, null)).isEmpty();
        }
    }

    // ── getTaskById ───────────────────────────────────────────────────────────

    @Nested @DisplayName("getTaskById()")
    class GetTaskById {

        @Test @DisplayName("returns task response when task belongs to user")
        void withValidOwnership_returnsTaskResponse() {
            Task entity = task();
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            assertThat(taskService.getTaskById(USER_ID, TASK_ID).getId()).isEqualTo(TASK_ID);
        }

        @Test @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(USER_ID, TASK_ID));
            verify(taskMapper, never()).toResponse(any());
        }
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Nested @DisplayName("updateTask()")
    class UpdateTask {

        @Test @DisplayName("updates all non-null fields")
        void withAllFields_updatesAllFields() {
            Task entity = task();
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, updateRequest());

            assertThat(entity.getTitle()).isEqualTo("Updated title");
            assertThat(entity.getPriority()).isEqualTo(Priority.LOW);
            verify(taskRepository).save(entity);
        }

        @Test @DisplayName("does not overwrite null fields — PATCH semantics")
        void withNullFields_leavesExistingValuesUntouched() {
            Task entity = task();
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, new UpdateTaskRequest());

            assertThat(entity.getTitle()).isEqualTo("Finish report");
            assertThat(entity.getPriority()).isEqualTo(Priority.HIGH);
        }

        @Test @DisplayName("clears categoryId when set to null")
        void withNullCategoryId_clearsCategoryId() {
            Task entity = task();
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setCategoryId(null);
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, req);

            assertThat(entity.getCategoryId()).isNull();
        }

        @Test @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(USER_ID, TASK_ID, updateRequest()));
            verify(taskRepository, never()).save(any());
        }
    }

    // ── completeTask ──────────────────────────────────────────────────────────

    @Nested @DisplayName("completeTask()")
    class CompleteTask {

        @Test @DisplayName("marks task as completed and sets completedAt")
        void withIncompleteTask_marksCompletedAndSetsTimestamp() {
            Task entity = task(); // isCompleted = false
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.completeTask(USER_ID, TASK_ID);

            assertThat(entity.isCompleted()).isTrue();
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(taskRepository).save(entity);
        }

        @Test @DisplayName("calls recalculateProgress after saving")
        void withIncompleteTask_callsProgressRecalculation() {
            Task entity = task();
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.completeTask(USER_ID, TASK_ID);

            verify(challengeProgressService).recalculateProgress(USER_ID);
        }

        @Test @DisplayName("completedAt is set to approximately now")
        void withIncompleteTask_completedAtIsNow() {
            Task entity = task();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.completeTask(USER_ID, TASK_ID);

            assertThat(entity.getCompletedAt()).isAfter(before);
        }

        @Test @DisplayName("throws TaskAlreadyCompletedException when task is already completed")
        void whenAlreadyCompleted_throwsTaskAlreadyCompletedException() {
            Task entity = task();
            entity.setCompleted(true);
            entity.setCompletedAt(LocalDateTime.now().minusDays(1));
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));

            assertThrows(TaskAlreadyCompletedException.class, () -> taskService.completeTask(USER_ID, TASK_ID));

            verify(taskRepository, never()).save(any());
            verify(challengeProgressService, never()).recalculateProgress(any());
        }

        @Test @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskService.completeTask(USER_ID, TASK_ID));
            verify(taskRepository, never()).save(any());
        }
    }

    // ── uncompleteTask ────────────────────────────────────────────────────────

    @Nested @DisplayName("uncompleteTask()")
    class UncompleteTask {

        @Test @DisplayName("marks task as not completed and clears completedAt")
        void withCompletedTask_clearsCompletionState() {
            Task entity = task();
            entity.setCompleted(true);
            entity.setCompletedAt(LocalDateTime.now().minusHours(2));
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.uncompleteTask(USER_ID, TASK_ID);

            assertThat(entity.isCompleted()).isFalse();
            assertThat(entity.getCompletedAt()).isNull();
            verify(taskRepository).save(entity);
        }

        @Test @DisplayName("calls recalculateProgress after saving")
        void withCompletedTask_callsProgressRecalculation() {
            Task entity = task();
            entity.setCompleted(true);
            entity.setCompletedAt(LocalDateTime.now().minusHours(1));
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.uncompleteTask(USER_ID, TASK_ID);

            verify(challengeProgressService).recalculateProgress(USER_ID);
        }

        @Test @DisplayName("throws TaskNotCompletedException when task is already incomplete")
        void whenAlreadyIncomplete_throwsTaskNotCompletedException() {
            Task entity = task(); // isCompleted = false
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));

            assertThrows(TaskNotCompletedException.class, () -> taskService.uncompleteTask(USER_ID, TASK_ID));

            verify(taskRepository, never()).save(any());
            verify(challengeProgressService, never()).recalculateProgress(any());
        }

        @Test @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskService.uncompleteTask(USER_ID, TASK_ID));
            verify(taskRepository, never()).save(any());
        }
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Nested @DisplayName("deleteTask()")
    class DeleteTask {

        @Test @DisplayName("soft-deletes task by setting isDeleted to true")
        void withValidTask_setsIsDeletedTrue() {
            Task entity = task();
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));

            taskService.deleteTask(USER_ID, TASK_ID);

            assertThat(entity.isDeleted()).isTrue();
            verify(taskRepository).save(entity);
        }

        @Test @DisplayName("does not physically remove the record")
        void withValidTask_doesNotCallRepositoryDelete() {
            Task entity = task();
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.of(entity));

            taskService.deleteTask(USER_ID, TASK_ID);

            verify(taskRepository, never()).delete(any());
            verify(taskRepository, never()).deleteById(any());
        }

        @Test @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(USER_ID, TASK_ID));
            verify(taskRepository, never()).save(any());
        }
    }

    // ── syncTasks ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("syncTasks()")
    class SyncTasks {

        @Test @DisplayName("returns tasks updated after given timestamp")
        void withValidTimestamp_returnsUpdatedTasks() {
            LocalDateTime since = LocalDateTime.now().minusHours(1);
            Task entity = task();
            when(taskRepository.findByUserIdUpdatedAfter(USER_ID, since)).thenReturn(List.of(entity));
            when(taskMapper.toResponseList(any())).thenReturn(List.of(taskResponse(entity)));

            assertThat(taskService.syncTasks(USER_ID, since)).hasSize(1);
        }

        @Test @DisplayName("returns empty list when nothing was updated")
        void whenNothingUpdated_returnsEmptyList() {
            LocalDateTime since = LocalDateTime.now();
            when(taskRepository.findByUserIdUpdatedAfter(USER_ID, since)).thenReturn(Collections.emptyList());
            when(taskMapper.toResponseList(any())).thenReturn(Collections.emptyList());

            assertThat(taskService.syncTasks(USER_ID, since)).isEmpty();
        }
    }
}