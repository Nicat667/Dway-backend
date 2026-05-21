package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.task.TaskNotFoundException;
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

    @InjectMocks private TaskService taskService;

    private static final UUID USER_ID     = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID TASK_ID     = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CATEGORY_ID = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");

    private Task task() {
        return Task.builder()
                .id(TASK_ID)
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .title("Finish report")
                .priority(Priority.HIGH)
                .period(Period.DAILY)
                .isCompleted(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private TaskResponse taskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .categoryId(task.getCategoryId())
                .title(task.getTitle())
                .priority(task.getPriority())
                .period(task.getPeriod())
                .isCompleted(task.isCompleted())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private CreateTaskRequest createRequest() {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle("Finish report");
        r.setPriority(Priority.HIGH);
        r.setCategoryId(CATEGORY_ID);
        r.setPeriod(Period.DAILY);
        return r;
    }

    private UpdateTaskRequest updateRequest() {
        UpdateTaskRequest r = new UpdateTaskRequest();
        r.setTitle("Updated title");
        r.setPriority(Priority.LOW);
        r.setPeriod(Period.WEEKLY);
        r.setDueDate(LocalDateTime.now().plusDays(3));
        r.setAlarmTime(LocalDateTime.now().plusDays(2));
        r.setNotes("Some notes");
        r.setCategoryId(CATEGORY_ID);
        return r;
    }

    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("saves task with correct userId and returns response")
        void withValidRequest_savesAndReturnsResponse() {
            Task entity = task();
            TaskResponse response = taskResponse(entity);
            CreateTaskRequest request = createRequest();

            when(taskMapper.toEntity(request)).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(response);

            TaskResponse result = taskService.createTask(USER_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TASK_ID);
            verify(taskRepository).save(entity);
            verify(taskMapper).toResponse(entity);
        }

        @Test
        @DisplayName("sets userId from parameter, not from request")
        void always_setsUserIdFromParameter() {
            Task entity = task();
            entity.setUserId(null); // mapper leaves it null
            CreateTaskRequest request = createRequest();

            when(taskMapper.toEntity(request)).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.createTask(USER_ID, request);

            assertThat(entity.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("persists task before returning response")
        void always_savesBeforeReturningResponse() {
            Task entity = task();
            CreateTaskRequest request = createRequest();

            when(taskMapper.toEntity(request)).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.createTask(USER_ID, request);

            InOrder order = inOrder(taskRepository, taskMapper);
            order.verify(taskRepository).save(entity);
            order.verify(taskMapper).toResponse(entity);
        }

        @Test
        @DisplayName("creates task with optional fields null when not provided")
        void withOnlyRequiredFields_createsTaskWithNullOptionals() {
            Task entity = Task.builder()
                    .id(TASK_ID)
                    .userId(USER_ID)
                    .categoryId(CATEGORY_ID)
                    .title("Minimal task")
                    .priority(Priority.MEDIUM)
                    .isCompleted(false)
                    .isDeleted(false)
                    .build();

            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle("Minimal task");
            request.setPriority(Priority.MEDIUM);
            request.setCategoryId(CATEGORY_ID);
            // period, dueDate, alarmTime, notes all null

            when(taskMapper.toEntity(request)).thenReturn(entity);
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            TaskResponse result = taskService.createTask(USER_ID, request);

            assertThat(result).isNotNull();
            verify(taskRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("returns all active tasks when no filters provided")
        void withNoFilters_returnsAllActiveTasks() {
            Task entity = task();
            List<Task> tasks = List.of(entity);
            List<TaskResponse> responses = List.of(taskResponse(entity));

            when(taskRepository.findAllActiveByUserId(USER_ID)).thenReturn(tasks);
            when(taskMapper.toResponseList(tasks)).thenReturn(responses);

            List<TaskResponse> result = taskService.getAllTasks(USER_ID, null, null);

            assertThat(result).hasSize(1);
            verify(taskRepository).findAllActiveByUserId(USER_ID);
            verify(taskRepository, never()).findByUserIdAndPeriod(any(), any());
            verify(taskRepository, never()).findByUserIdAndCategoryId(any(), any());
        }

        @Test
        @DisplayName("filters by period when period is provided")
        void withPeriod_callsFindByUserIdAndPeriod() {
            Task entity = task();
            List<Task> tasks = List.of(entity);
            List<TaskResponse> responses = List.of(taskResponse(entity));

            when(taskRepository.findByUserIdAndPeriod(USER_ID, "DAILY")).thenReturn(tasks);
            when(taskMapper.toResponseList(tasks)).thenReturn(responses);

            List<TaskResponse> result = taskService.getAllTasks(USER_ID, Period.DAILY, null);

            assertThat(result).hasSize(1);
            verify(taskRepository).findByUserIdAndPeriod(USER_ID, "DAILY");
            verify(taskRepository, never()).findAllActiveByUserId(any());
            verify(taskRepository, never()).findByUserIdAndCategoryId(any(), any());
        }

        @Test
        @DisplayName("filters by categoryId when categoryId is provided")
        void withCategoryId_callsFindByUserIdAndCategoryId() {
            Task entity = task();
            List<Task> tasks = List.of(entity);
            List<TaskResponse> responses = List.of(taskResponse(entity));

            when(taskRepository.findByUserIdAndCategoryId(USER_ID, CATEGORY_ID)).thenReturn(tasks);
            when(taskMapper.toResponseList(tasks)).thenReturn(responses);

            List<TaskResponse> result = taskService.getAllTasks(USER_ID, null, CATEGORY_ID);

            assertThat(result).hasSize(1);
            verify(taskRepository).findByUserIdAndCategoryId(USER_ID, CATEGORY_ID);
            verify(taskRepository, never()).findAllActiveByUserId(any());
            verify(taskRepository, never()).findByUserIdAndPeriod(any(), any());
        }

        @Test
        @DisplayName("period filter takes precedence over categoryId when both provided")
        void withBothFilters_periodTakesPrecedence() {
            List<Task> tasks = List.of(task());

            when(taskRepository.findByUserIdAndPeriod(USER_ID, "WEEKLY")).thenReturn(tasks);
            when(taskMapper.toResponseList(tasks)).thenReturn(List.of());

            taskService.getAllTasks(USER_ID, Period.WEEKLY, CATEGORY_ID);

            verify(taskRepository).findByUserIdAndPeriod(USER_ID, "WEEKLY");
            verify(taskRepository, never()).findByUserIdAndCategoryId(any(), any());
            verify(taskRepository, never()).findAllActiveByUserId(any());
        }

        @Test
        @DisplayName("returns empty list when user has no tasks")
        void whenNoTasksExist_returnsEmptyList() {
            when(taskRepository.findAllActiveByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(taskMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<TaskResponse> result = taskService.getAllTasks(USER_ID, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("passes period name as string to repository")
        void withPeriod_passesEnumNameAsString() {
            when(taskRepository.findByUserIdAndPeriod(USER_ID, "MONTHLY")).thenReturn(List.of());
            when(taskMapper.toResponseList(any())).thenReturn(List.of());

            taskService.getAllTasks(USER_ID, Period.MONTHLY, null);

            verify(taskRepository).findByUserIdAndPeriod(USER_ID, "MONTHLY");
        }
    }

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskById {

        @Test
        @DisplayName("returns task response when task belongs to user")
        void withValidOwnership_returnsTaskResponse() {
            Task entity = task();
            TaskResponse response = taskResponse(entity);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(response);

            TaskResponse result = taskService.getTaskById(USER_ID, TASK_ID);

            assertThat(result.getId()).isEqualTo(TASK_ID);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.getTaskById(USER_ID, TASK_ID));

            verify(taskMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task belongs to different user")
        void whenTaskBelongsToDifferentUser_throwsTaskNotFoundException() {
            UUID otherUser = UUID.randomUUID();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, otherUser))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.getTaskById(otherUser, TASK_ID));
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task is soft-deleted")
        void whenTaskIsSoftDeleted_throwsTaskNotFoundException() {
            // findByIdAndUserIdAndIsDeletedFalse excludes deleted tasks
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.getTaskById(USER_ID, TASK_ID));
        }
    }

    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("updates all non-null fields when all are provided")
        void withAllFields_updatesAllFields() {
            Task entity = task();
            UpdateTaskRequest request = updateRequest();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getTitle()).isEqualTo("Updated title");
            assertThat(entity.getPriority()).isEqualTo(Priority.LOW);
            assertThat(entity.getPeriod()).isEqualTo(Period.WEEKLY);
            assertThat(entity.getDueDate()).isNotNull();
            assertThat(entity.getAlarmTime()).isNotNull();
            assertThat(entity.getNotes()).isEqualTo("Some notes");
            assertThat(entity.getCategoryId()).isEqualTo(CATEGORY_ID);
            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("does not update title when title is null")
        void withNullTitle_titleUnchanged() {
            Task entity = task();
            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setTitle(null);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getTitle()).isEqualTo("Finish report");
        }

        @Test
        @DisplayName("does not update priority when priority is null")
        void withNullPriority_priorityUnchanged() {
            Task entity = task();
            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setPriority(null);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getPriority()).isEqualTo(Priority.HIGH);
        }

        @Test
        @DisplayName("does not update period when period is null")
        void withNullPeriod_periodUnchanged() {
            Task entity = task();
            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setPeriod(null);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getPeriod()).isEqualTo(Period.DAILY);
        }

        @Test
        @DisplayName("does not update dueDate when dueDate is null")
        void withNullDueDate_dueDateUnchanged() {
            Task entity = task();
            LocalDateTime original = LocalDateTime.now().plusDays(1);
            entity.setDueDate(original);

            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setDueDate(null);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getDueDate()).isEqualTo(original);
        }

        @Test
        @DisplayName("does not update alarmTime when alarmTime is null")
        void withNullAlarmTime_alarmTimeUnchanged() {
            Task entity = task();
            LocalDateTime original = LocalDateTime.now().plusHours(5);
            entity.setAlarmTime(original);

            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setAlarmTime(null);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getAlarmTime()).isEqualTo(original);
        }

        @Test
        @DisplayName("does not update notes when notes is null")
        void withNullNotes_notesUnchanged() {
            Task entity = task();
            entity.setNotes("Original notes");

            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setNotes(null);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getNotes()).isEqualTo("Original notes");
        }

        @Test
        @DisplayName("clears categoryId when categoryId is null in request")
        void withNullCategoryId_clearsCategoryId() {
            Task entity = task(); // has CATEGORY_ID set

            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setCategoryId(null); // explicitly null → should clear

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getCategoryId()).isNull();
        }

        @Test
        @DisplayName("updates categoryId when new categoryId is provided")
        void withNewCategoryId_updatesCategoryId() {
            Task entity = task();
            UUID newCategoryId = UUID.randomUUID();

            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setCategoryId(newCategoryId);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, request);

            assertThat(entity.getCategoryId()).isEqualTo(newCategoryId);
        }

        @Test
        @DisplayName("saves task after applying updates")
        void always_savesTaskAfterUpdate() {
            Task entity = task();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.updateTask(USER_ID, TASK_ID, new UpdateTaskRequest());

            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.updateTask(USER_ID, TASK_ID, updateRequest()));

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task belongs to different user")
        void whenTaskBelongsToDifferentUser_throwsTaskNotFoundException() {
            UUID otherUser = UUID.randomUUID();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, otherUser))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.updateTask(otherUser, TASK_ID, updateRequest()));

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {

        @Test
        @DisplayName("marks task as completed and sets completedAt")
        void withValidTask_marksCompletedAndSetsTimestamp() {
            Task entity = task(); // isCompleted = false

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.completeTask(USER_ID, TASK_ID);

            assertThat(entity.isCompleted()).isTrue();
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("completedAt is set to approximately now")
        void withValidTask_completedAtIsNow() {
            Task entity = task();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.completeTask(USER_ID, TASK_ID);

            assertThat(entity.getCompletedAt()).isAfter(before);
        }

        @Test
        @DisplayName("completing already-completed task is idempotent")
        void whenAlreadyCompleted_overwritesTimestampAndSaves() {
            Task entity = task();
            entity.setCompleted(true);
            entity.setCompletedAt(LocalDateTime.now().minusDays(1));

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.completeTask(USER_ID, TASK_ID);

            assertThat(entity.isCompleted()).isTrue();
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.completeTask(USER_ID, TASK_ID));

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task belongs to different user")
        void whenTaskBelongsToDifferentUser_throwsTaskNotFoundException() {
            UUID otherUser = UUID.randomUUID();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, otherUser))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.completeTask(otherUser, TASK_ID));

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("uncompleteTask()")
    class UncompleteTask {

        @Test
        @DisplayName("marks task as not completed and clears completedAt")
        void withCompletedTask_clearsCompletionState() {
            Task entity = task();
            entity.setCompleted(true);
            entity.setCompletedAt(LocalDateTime.now().minusHours(2));

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.uncompleteTask(USER_ID, TASK_ID);

            assertThat(entity.isCompleted()).isFalse();
            assertThat(entity.getCompletedAt()).isNull();
            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("uncompleting already-incomplete task is idempotent")
        void whenAlreadyIncomplete_stillSaves() {
            Task entity = task(); // isCompleted = false, completedAt = null

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(taskMapper.toResponse(entity)).thenReturn(taskResponse(entity));

            taskService.uncompleteTask(USER_ID, TASK_ID);

            assertThat(entity.isCompleted()).isFalse();
            assertThat(entity.getCompletedAt()).isNull();
            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.uncompleteTask(USER_ID, TASK_ID));

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task belongs to different user")
        void whenTaskBelongsToDifferentUser_throwsTaskNotFoundException() {
            UUID otherUser = UUID.randomUUID();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, otherUser))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.uncompleteTask(otherUser, TASK_ID));

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteTask()")
    class DeleteTask {

        @Test
        @DisplayName("soft-deletes task by setting isDeleted to true")
        void withValidTask_setsIsDeletedTrue() {
            Task entity = task();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));

            taskService.deleteTask(USER_ID, TASK_ID);

            assertThat(entity.isDeleted()).isTrue();
            verify(taskRepository).save(entity);
        }

        @Test
        @DisplayName("does not physically remove the record from the database")
        void withValidTask_doesNotCallRepositoryDelete() {
            Task entity = task();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.of(entity));

            taskService.deleteTask(USER_ID, TASK_ID);

            verify(taskRepository, never()).delete(any());
            verify(taskRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task does not exist")
        void whenTaskNotFound_throwsTaskNotFoundException() {
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.deleteTask(USER_ID, TASK_ID));

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task belongs to different user")
        void whenTaskBelongsToDifferentUser_throwsTaskNotFoundException() {
            UUID otherUser = UUID.randomUUID();

            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, otherUser))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.deleteTask(otherUser, TASK_ID));

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task is already soft-deleted")
        void whenTaskAlreadyDeleted_throwsTaskNotFoundException() {
            // findByIdAndUserIdAndIsDeletedFalse returns empty for already-deleted tasks
            when(taskRepository.findByIdAndUserIdAndIsDeletedFalse(TASK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.deleteTask(USER_ID, TASK_ID));
        }
    }

    @Nested
    @DisplayName("syncTasks()")
    class SyncTasks {

        @Test
        @DisplayName("returns tasks updated after given timestamp")
        void withValidTimestamp_returnsUpdatedTasks() {
            LocalDateTime since = LocalDateTime.now().minusHours(1);
            Task entity = task();
            List<Task> tasks = List.of(entity);
            List<TaskResponse> responses = List.of(taskResponse(entity));

            when(taskRepository.findByUserIdUpdatedAfter(USER_ID, since)).thenReturn(tasks);
            when(taskMapper.toResponseList(tasks)).thenReturn(responses);

            List<TaskResponse> result = taskService.syncTasks(USER_ID, since);

            assertThat(result).hasSize(1);
            verify(taskRepository).findByUserIdUpdatedAfter(USER_ID, since);
        }

        @Test
        @DisplayName("returns empty list when nothing was updated after given timestamp")
        void whenNothingUpdated_returnsEmptyList() {
            LocalDateTime since = LocalDateTime.now();

            when(taskRepository.findByUserIdUpdatedAfter(USER_ID, since))
                    .thenReturn(Collections.emptyList());
            when(taskMapper.toResponseList(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            List<TaskResponse> result = taskService.syncTasks(USER_ID, since);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("passes exact timestamp to repository without modification")
        void always_passesExactTimestampToRepository() {
            LocalDateTime since = LocalDateTime.of(2026, 5, 1, 10, 30, 0);

            when(taskRepository.findByUserIdUpdatedAfter(USER_ID, since))
                    .thenReturn(Collections.emptyList());
            when(taskMapper.toResponseList(any())).thenReturn(Collections.emptyList());

            taskService.syncTasks(USER_ID, since);

            verify(taskRepository).findByUserIdUpdatedAfter(USER_ID, since);
        }

        @Test
        @DisplayName("includes soft-deleted tasks in sync results")
        void withDeletedTasks_includesThemInResults() {
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            Task deletedTask = task();
            deletedTask.setDeleted(true);
            List<Task> tasks = List.of(deletedTask);

            when(taskRepository.findByUserIdUpdatedAfter(USER_ID, since)).thenReturn(tasks);
            when(taskMapper.toResponseList(tasks)).thenReturn(List.of(taskResponse(deletedTask)));

            List<TaskResponse> result = taskService.syncTasks(USER_ID, since);

            // sync uses findByUserIdUpdatedAfter which does NOT filter by isDeleted
            // so deleted tasks are included — client needs to remove them locally
            assertThat(result).hasSize(1);
        }
    }
}