package com.dway.dwaybackend.dto.request.partner;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePartnerRequest {

    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    private String description;

    @Size(max = 255, message = "Discount text must be at most 255 characters")
    private String discountText;

    @Size(max = 255, message = "Promo code must be at most 255 characters")
    private String promoCode;

    @Size(max = 1000, message = "Partner URL must be at most 1000 characters")
    private String partnerUrl;

    private Boolean isActive;
}