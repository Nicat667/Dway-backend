package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.service.mobile.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Partners")
@RestController
@RequestMapping("/api/v1/mobile/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "Get all active partners, ordered by creation date descending")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PartnerResponse>>> getActivePartners() {
        return ResponseEntity.ok(ApiResponse.success(null, partnerService.getActivePartners()));
    }
}