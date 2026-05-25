package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.response.challenge.ChallengeResponse;
import com.dway.dwaybackend.security.CurrentUser;
import com.dway.dwaybackend.service.mobile.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Challenges")
@RestController
@RequestMapping("/api/v1/mobile/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "Get all active challenges with current user's join status and progress")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getAllChallenges(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(null, challengeService.getAllChallenges(userId)));
    }

    @Operation(summary = "Join a challenge")
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<ChallengeResponse>> joinChallenge(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Joined challenge", challengeService.joinChallenge(userId, id)));
    }

    @Operation(summary = "Leave a challenge")
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveChallenge(@CurrentUser UUID userId, @PathVariable UUID id) {
        challengeService.leaveChallenge(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Left challenge", null));
    }
}