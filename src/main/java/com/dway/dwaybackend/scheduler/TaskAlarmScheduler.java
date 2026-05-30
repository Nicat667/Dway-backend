package com.dway.dwaybackend.scheduler;

import com.dway.dwaybackend.entity.Task;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.infrastructure.firebase.FcmService;
import com.dway.dwaybackend.repository.TaskRepository;
import com.dway.dwaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAlarmScheduler {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void fireTaskAlarms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusSeconds(60);

        List<Task> tasks = taskRepository.findPendingAlarms(now, windowEnd);
        if (tasks.isEmpty()) return;

        log.info("[Alarm] Firing {} task alarm(s), window [{} → {}]",
                tasks.size(), now, windowEnd);

        // One DB round-trip to get all users involved — not one query per task
        Set<UUID> userIds = tasks.stream()
                .map(Task::getUserId)
                .collect(Collectors.toSet());

        Map<UUID, String> tokenByUserId = userRepository.findAllById(userIds)
                .stream()
                .filter(u -> u.getPushToken() != null && !u.getPushToken().isBlank())
                .collect(Collectors.toMap(User::getId, User::getPushToken));

        for (Task task : tasks) {
            try {
                String token = tokenByUserId.get(task.getUserId());
                if (token != null) {
                    fcmService.sendNotification(
                            token,
                            "Task Reminder ⏰",
                            "Time for: " + task.getTitle()
                    );
                }
                // Mark sent whether or not user has a token —
                // prevents infinite re-query on tokenless users
                task.setAlarmSent(true);
                taskRepository.save(task);
            } catch (Exception e) {
                log.error("[Alarm] Error processing alarm for task {}: {}",
                        task.getId(), e.getMessage());
            }
        }
    }
}