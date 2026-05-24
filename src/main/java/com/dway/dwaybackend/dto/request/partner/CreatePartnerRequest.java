package com.dway.dwaybackend.dto.request.partner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePartnerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Discount text is required")
    @Size(max = 255, message = "Discount text must be at most 255 characters")
    private String discountText;

    @NotBlank(message = "Promo code is required")
    @Size(max = 255, message = "Promo code must be at most 255 characters")
    private String promoCode;

    @NotBlank(message = "Partner URL is required")
    @Size(max = 1000, message = "Partner URL must be at most 1000 characters")
    private String partnerUrl;

    private Boolean isActive;
}