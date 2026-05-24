package com.dway.dwaybackend.dto.request.motivation;

import lombok.Data;

// PATCH semantics — only non-null fields are applied.
@Data
public class UpdateMotivationRequest {

    private String quote;
    private String author;
}