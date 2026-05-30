package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.response.ApiResponse;
import com.dway.dwaybackend.dto.request.partner.CreatePartnerRequest;
import com.dway.dwaybackend.dto.request.partner.UpdatePartnerRequest;
import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.service.admin.AdminPartnerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

@Tag(name = "Admin — Partners")
@RestController
@RequestMapping("/api/v1/admin/partners")
@RequiredArgsConstructor
public class AdminPartnerController {

    private final AdminPartnerService adminPartnerService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(summary = "Get all partners paginated, ordered by creation date descending. Optional isActive filter.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PartnerResponse>>> getAllPartners(@RequestParam(required = false) Boolean isActive, @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(null, adminPartnerService.getAllPartners(isActive, pageable)));
    }

    @Operation(summary = "Get a single partner by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartnerResponse>> getPartnerById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(null, adminPartnerService.getPartnerById(id)));
    }

    
    @Operation(summary = "Create a partner — multipart/form-data: 'file' (icon image) + 'data' (JSON string)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PartnerResponse>> createPartner(@RequestPart("file") MultipartFile file, @RequestParam("data") String rawData) {
        CreatePartnerRequest data = parseAndValidate(rawData);
        return ResponseEntity.ok(ApiResponse.success("Partner created", adminPartnerService.createPartner(data, file)));
    }

    private CreatePartnerRequest parseAndValidate(String rawData) {
        CreatePartnerRequest request;
        try {
            request = objectMapper.readValue(rawData, CreatePartnerRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in 'data' parameter: " + e.getOriginalMessage());
        }
        Set<ConstraintViolation<CreatePartnerRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            ConstraintViolation<CreatePartnerRequest> first = violations.iterator().next();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    first.getPropertyPath() + ": " + first.getMessage());
        }
        return request;
    }

    @Operation(summary = "Update partner text fields — PATCH semantics, only non-null fields applied. Does not touch the icon.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PartnerResponse>> updatePartner(@PathVariable UUID id, @RequestBody @Valid UpdatePartnerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Partner updated", adminPartnerService.updatePartner(id, request)));
    }

    @Operation(summary = "Replace the partner's icon image")
    @PostMapping(value = "/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PartnerResponse>> updateIcon(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Partner icon updated", adminPartnerService.updateIcon(id, file)));
    }

    @Operation(summary = "Delete a partner and its icon from S3")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePartner(@PathVariable UUID id) {
        adminPartnerService.deletePartner(id);
        return ResponseEntity.ok(ApiResponse.success("Partner deleted", null));
    }
}