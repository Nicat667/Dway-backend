package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.exception.task.TaskNotFoundException;
import com.dway.dwaybackend.dto.response.task.TaskResponse;
import com.dway.dwaybackend.entity.enums.Period;
import com.dway.dwaybackend.entity.enums.Priority;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.service.mobile.TaskService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@DisplayName("TaskController")
class TaskControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TaskService taskService;
    @MockitoBean RateLimitService rateLimitService;

    private static final UUID USER_ID = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID TASK_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CATEGORY_ID = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");
    private static final String BASE = "/api/v1/mobile/tasks";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    // Sets authentication principal to UUID — required for @CurrentUser to resolve correctly
    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private TaskResponse stubTask() {
        return TaskResponse.builder()
                .id(TASK_ID)
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .title("Finish report")
                .priority(Priority.HIGH)
                .period(Period.DAILY)
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST / — createTask")
    class CreateTask {

        @Test
        @DisplayName("200 returns created task with message")
        void createTask_success() throws Exception {
            when(taskService.createTask(eq(USER_ID), any())).thenReturn(stubTask());

            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Finish report",
                                      "priority": "HIGH",
                                      "categoryId": "%s"
                                    }
                                    """.formatted(CATEGORY_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task created"))
                    .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                    .andExpect(jsonPath("$.data.title").value("Finish report"));
        }

        @Test
        @DisplayName("200 with optional fields (period, dueDate, alarmTime, notes)")
        void createTask_withAllOptionalFields() throws Exception {
            when(taskService.createTask(eq(USER_ID), any())).thenReturn(stubTask());

            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Finish report",
                                      "priority": "HIGH",
                                      "categoryId": "%s",
                                      "period": "DAILY",
                                      "notes": "Important!"
                                    }
                                    """.formatted(CATEGORY_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("400 when title is blank")
        void createTask_blankTitle() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "",
                                      "priority": "HIGH",
                                      "categoryId": "%s"
                                    }
                                    """.formatted(CATEGORY_ID)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any(), any());
        }

        @Test
        @DisplayName("400 when title is missing")
        void createTask_missingTitle() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "priority": "HIGH",
                                      "categoryId": "%s"
                                    }
                                    """.formatted(CATEGORY_ID)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any(), any());
        }

        @Test
        @DisplayName("400 when priority is missing")
        void createTask_missingPriority() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Finish report",
                                      "categoryId": "%s"
                                    }
                                    """.formatted(CATEGORY_ID)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any(), any());
        }

        @Test
        @DisplayName("400 when categoryId is missing")
        void createTask_missingCategoryId() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Finish report",
                                      "priority": "HIGH"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void createTask_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Finish report",
                                      "priority": "HIGH",
                                      "categoryId": "%s"
                                    }
                                    """.formatted(CATEGORY_ID)))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).createTask(any(), any());
        }
    }

    @Nested
    @DisplayName("GET / — getAllTasks")
    class GetAllTasks {

        @Test
        @DisplayName("200 returns list of tasks with no filters")
        void getAllTasks_noFilters() throws Exception {
            when(taskService.getAllTasks(USER_ID, null, null)).thenReturn(List.of(stubTask()));

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(TASK_ID.toString()));
        }

        @Test
        @DisplayName("200 returns empty list when user has no tasks")
        void getAllTasks_emptyList() throws Exception {
            when(taskService.getAllTasks(USER_ID, null, null)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("200 passes period param to service")
        void getAllTasks_withPeriod() throws Exception {
            when(taskService.getAllTasks(USER_ID, Period.DAILY, null)).thenReturn(List.of(stubTask()));

            mockMvc.perform(get(BASE).param("period", "DAILY").with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(taskService).getAllTasks(USER_ID, Period.DAILY, null);
        }

        @Test
        @DisplayName("200 passes categoryId param to service")
        void getAllTasks_withCategoryId() throws Exception {
            when(taskService.getAllTasks(USER_ID, null, CATEGORY_ID)).thenReturn(List.of(stubTask()));

            mockMvc.perform(get(BASE).param("categoryId", CATEGORY_ID.toString()).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(taskService).getAllTasks(USER_ID, null, CATEGORY_ID);
        }

        @Test
        @DisplayName("200 passes both period and categoryId — service decides priority")
        void getAllTasks_withBothFilters() throws Exception {
            when(taskService.getAllTasks(USER_ID, Period.WEEKLY, CATEGORY_ID))
                    .thenReturn(List.of(stubTask()));

            mockMvc.perform(get(BASE)
                            .param("period", "WEEKLY")
                            .param("categoryId", CATEGORY_ID.toString())
                            .with(asUser()))
                    .andExpect(status().isOk());

            verify(taskService).getAllTasks(USER_ID, Period.WEEKLY, CATEGORY_ID);
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getAllTasks_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).getAllTasks(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /sync — syncTasks")
    class SyncTasks {

        @Test
        @DisplayName("200 returns tasks updated after given timestamp")
        void syncTasks_success() throws Exception {
            String since = "2026-05-01T10:30:00";
            LocalDateTime sinceTs = LocalDateTime.parse(since);

            when(taskService.syncTasks(USER_ID, sinceTs)).thenReturn(List.of(stubTask()));

            mockMvc.perform(get(BASE + "/sync").param("since", since).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(TASK_ID.toString()));
        }

        @Test
        @DisplayName("200 returns empty list when nothing was updated")
        void syncTasks_emptyResult() throws Exception {
            String since = "2026-05-20T00:00:00";
            when(taskService.syncTasks(eq(USER_ID), any())).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/sync").param("since", since).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("400 when since param is missing (required)")
        void syncTasks_missingSince() throws Exception {
            mockMvc.perform(get(BASE + "/sync").with(asUser()))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).syncTasks(any(), any());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void syncTasks_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE + "/sync").param("since", "2026-05-01T10:30:00"))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).syncTasks(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /{id} — getTaskById")
    class GetTaskById {

        @Test
        @DisplayName("200 returns task when found")
        void getTaskById_success() throws Exception {
            when(taskService.getTaskById(USER_ID, TASK_ID)).thenReturn(stubTask());

            mockMvc.perform(get(BASE + "/" + TASK_ID).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()))
                    .andExpect(jsonPath("$.data.title").value("Finish report"));
        }

        @Test
        @DisplayName("404 when task not found or not owned by user")
        void getTaskById_notFound() throws Exception {
            when(taskService.getTaskById(USER_ID, TASK_ID)).thenThrow(new TaskNotFoundException());

            mockMvc.perform(get(BASE + "/" + TASK_ID).with(asUser()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getTaskById_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE + "/" + TASK_ID))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).getTaskById(any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /{id} — updateTask")
    class UpdateTask {

        @Test
        @DisplayName("200 returns updated task with message")
        void updateTask_success() throws Exception {
            when(taskService.updateTask(eq(USER_ID), eq(TASK_ID), any())).thenReturn(stubTask());

            mockMvc.perform(patch(BASE + "/" + TASK_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "title": "Updated title", "priority": "LOW" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task updated"))
                    .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()));
        }

        @Test
        @DisplayName("200 with empty body — all fields are optional (PATCH semantics)")
        void updateTask_emptyBody() throws Exception {
            when(taskService.updateTask(eq(USER_ID), eq(TASK_ID), any())).thenReturn(stubTask());

            mockMvc.perform(patch(BASE + "/" + TASK_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("200 with null categoryId — removes category association")
        void updateTask_nullCategoryIdRemovesCategory() throws Exception {
            when(taskService.updateTask(eq(USER_ID), eq(TASK_ID), any())).thenReturn(stubTask());

            mockMvc.perform(patch(BASE + "/" + TASK_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "categoryId": null }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("404 when task not found or not owned by user")
        void updateTask_notFound() throws Exception {
            when(taskService.updateTask(eq(USER_ID), eq(TASK_ID), any()))
                    .thenThrow(new TaskNotFoundException());

            mockMvc.perform(patch(BASE + "/" + TASK_ID)
                            .with(asUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "title": "Updated title" }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void updateTask_unauthenticated() throws Exception {
            mockMvc.perform(patch(BASE + "/" + TASK_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "title": "Updated title" }
                                    """))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).updateTask(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /{id}/complete — completeTask")
    class CompleteTask {

        @Test
        @DisplayName("200 returns completed task with message")
        void completeTask_success() throws Exception {
            TaskResponse completed = stubTask();
            completed.setIsCompleted(true);
            completed.setCompletedAt(LocalDateTime.now());

            when(taskService.completeTask(USER_ID, TASK_ID)).thenReturn(completed);

            mockMvc.perform(post(BASE + "/" + TASK_ID + "/complete")
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task completed"))
                    .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()));
        }

        @Test
        @DisplayName("404 when task not found or not owned by user")
        void completeTask_notFound() throws Exception {
            when(taskService.completeTask(USER_ID, TASK_ID)).thenThrow(new TaskNotFoundException());

            mockMvc.perform(post(BASE + "/" + TASK_ID + "/complete")
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void completeTask_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE + "/" + TASK_ID + "/complete").with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).completeTask(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /{id}/uncomplete — uncompleteTask")
    class UncompleteTask {

        @Test
        @DisplayName("200 returns uncompleted task with message")
        void uncompleteTask_success() throws Exception {
            when(taskService.uncompleteTask(USER_ID, TASK_ID)).thenReturn(stubTask());

            mockMvc.perform(post(BASE + "/" + TASK_ID + "/uncomplete")
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task uncompleted"))
                    .andExpect(jsonPath("$.data.id").value(TASK_ID.toString()));
        }

        @Test
        @DisplayName("404 when task not found or not owned by user")
        void uncompleteTask_notFound() throws Exception {
            when(taskService.uncompleteTask(USER_ID, TASK_ID)).thenThrow(new TaskNotFoundException());

            mockMvc.perform(post(BASE + "/" + TASK_ID + "/uncomplete")
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void uncompleteTask_unauthenticated() throws Exception {
            mockMvc.perform(post(BASE + "/" + TASK_ID + "/uncomplete").with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).uncompleteTask(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /{id} — deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("200 returns success message with null data")
        void deleteTask_success() throws Exception {
            doNothing().when(taskService).deleteTask(USER_ID, TASK_ID);

            mockMvc.perform(delete(BASE + "/" + TASK_ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task deleted"));
        }

        @Test
        @DisplayName("404 when task not found or not owned by user")
        void deleteTask_notFound() throws Exception {
            doThrow(new TaskNotFoundException()).when(taskService).deleteTask(USER_ID, TASK_ID);

            mockMvc.perform(delete(BASE + "/" + TASK_ID)
                            .with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void deleteTask_unauthenticated() throws Exception {
            mockMvc.perform(delete(BASE + "/" + TASK_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(taskService, never()).deleteTask(any(), any());
        }
    }
}