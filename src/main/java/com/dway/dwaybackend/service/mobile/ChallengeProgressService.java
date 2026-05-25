package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.entity.Challenge;
import com.dway.dwaybackend.entity.UserChallenge;
import com.dway.dwaybackend.repository.ChallengeRepository;
import com.dway.dwaybackend.repository.TaskRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import com.dway.dwaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeProgressService {

    private final UserChallengeRepository userChallengeRepository;
    private final ChallengeRepository challengeRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recalculateProgress(UUID userId) {
        List<UserChallenge> incomplete =
                userChallengeRepository.findByUserIdAndCompletedAtIsNull(userId);

        if (incomplete.isEmpty()) return;

        Set<UUID> challengeIds = incomplete.stream()
                .map(UserChallenge::getChallengeId)
                .collect(Collectors.toSet());

        Map<UUID, Challenge> challengeMap = challengeRepository.findAllById(challengeIds)
                .stream()
                .collect(Collectors.toMap(Challenge::getId, Function.identity()));

        for (UserChallenge uc : incomplete) {
            Challenge challenge = challengeMap.get(uc.getChallengeId());
            if (challenge == null || !challenge.isActive()) continue;

            long completedSinceJoined =
                    taskRepository.countCompletedByUserIdSince(userId, uc.getJoinedAt());

            int newProgress = (int) Math.min(completedSinceJoined, challenge.getTargetCount());

            if (newProgress == uc.getProgress()) continue;

            uc.setProgress(newProgress);

            if (newProgress >= challenge.getTargetCount()) {
                uc.setCompletedAt(LocalDateTime.now());
                userRepository.incrementPoints(userId, challenge.getRewardPoints());
                log.info("User {} completed challenge '{}' — awarded {} pts",
                        userId, challenge.getTitle(), challenge.getRewardPoints());
            }

            userChallengeRepository.save(uc);
        }
    }
}