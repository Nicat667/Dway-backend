package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.challenge.AlreadyJoinedException;
import com.dway.dwaybackend.common.exception.challenge.ChallengeExpiredException;
import com.dway.dwaybackend.common.exception.challenge.ChallengeNotFoundException;
import com.dway.dwaybackend.common.exception.challenge.NotJoinedException;
import com.dway.dwaybackend.dto.response.challenge.ChallengeResponse;
import com.dway.dwaybackend.entity.Challenge;
import com.dway.dwaybackend.entity.UserChallenge;
import com.dway.dwaybackend.mapper.ChallengeMapper;
import com.dway.dwaybackend.repository.ChallengeRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final ChallengeMapper challengeMapper;

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getAllChallenges(UUID userId) {
        List<Challenge> challenges = challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc();

        List<UserChallenge> userChallenges = userChallengeRepository.findByUserId(userId);
        Map<UUID, UserChallenge> ucByChallenge = userChallenges.stream()
                .collect(Collectors.toMap(UserChallenge::getChallengeId, Function.identity()));

        return challenges.stream()
                .map(challenge -> {
                    ChallengeResponse base = challengeMapper.toResponse(challenge);
                    UserChallenge uc = ucByChallenge.get(challenge.getId());
                    return ChallengeResponse.builder()
                            .id(base.getId())
                            .icon(base.getIcon())
                            .title(base.getTitle())
                            .description(base.getDescription())
                            .difficulty(base.getDifficulty())
                            .targetCount(base.getTargetCount())
                            .rewardPoints(base.getRewardPoints())
                            .participantCount(base.getParticipantCount())
                            .expiresAt(base.getExpiresAt())
                            .isJoined(ucByChallenge.containsKey(challenge.getId()))
                            .progress(uc != null ? uc.getProgress() : 0)
                            .completedAt(uc != null ? uc.getCompletedAt() : null)
                            .joinedAt(uc != null ? uc.getJoinedAt() : null)
                            .createdAt(base.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ChallengeResponse joinChallenge(UUID userId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(ChallengeNotFoundException::new);

        if (challenge.getExpiresAt() != null && LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
            throw new ChallengeExpiredException();
        }

        if (userChallengeRepository.existsByUserIdAndChallengeId(userId, challengeId)) {
            throw new AlreadyJoinedException();
        }

        LocalDateTime now = LocalDateTime.now();
        UserChallenge uc = UserChallenge.builder()
                .userId(userId)
                .challengeId(challengeId)
                .joinedAt(now)
                .build();
        userChallengeRepository.save(uc);

        challengeRepository.incrementParticipantCount(challengeId);
        challenge.setParticipantCount(challenge.getParticipantCount() + 1);
        log.info("User {} joined challenge {}", userId, challengeId);

        return ChallengeResponse.builder()
                .id(challenge.getId())
                .icon(challenge.getIcon())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .difficulty(challenge.getDifficulty())
                .targetCount(challenge.getTargetCount())
                .rewardPoints(challenge.getRewardPoints())
                .participantCount(challenge.getParticipantCount())
                .expiresAt(challenge.getExpiresAt())
                .isJoined(true)
                .progress(0)
                .joinedAt(now)
                .createdAt(challenge.getCreatedAt())
                .build();
    }

    @Transactional
    public void leaveChallenge(UUID userId, UUID challengeId) {
        if (!challengeRepository.existsById(challengeId)) throw new ChallengeNotFoundException();

        UserChallenge uc = userChallengeRepository
                .findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(NotJoinedException::new);

        userChallengeRepository.delete(uc);
        challengeRepository.decrementParticipantCount(challengeId);
        log.info("User {} left challenge {}", userId, challengeId);
    }
}