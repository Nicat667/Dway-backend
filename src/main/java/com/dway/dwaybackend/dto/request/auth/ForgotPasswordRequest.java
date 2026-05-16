package com.dway.dwaybackend.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}