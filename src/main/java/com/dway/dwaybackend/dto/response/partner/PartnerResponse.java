package com.dway.dwaybackend.dto.response.partner;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PartnerResponse {

    private UUID id;
    private String name;
    private String iconUrl;
    private String description;
    private String discountText;
    private String promoCode;
    private String partnerUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}