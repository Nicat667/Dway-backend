package com.dway.dwaybackend.dto.request.motivation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMotivationRequest {

    @NotBlank(message = "Quote is required")
    private String quote;

    private String author;
}