package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.service.mobile.MotivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Motivations")
@RestController
@RequestMapping("/api/v1/mobile/motivations")
@RequiredArgsConstructor
public class MotivationController {

    private final MotivationService motivationService;

    @Operation(summary = "Get today's daily motivation. Falls back to a random past quote if today has no planned entry.")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<MotivationResponse>> getTodayMotivation() {
        return ResponseEntity.ok(ApiResponse.success(null, motivationService.getTodayMotivation()));
    }
}