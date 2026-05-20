package com.dway.dwaybackend.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 60, message = "Name must be between 2 and 60 characters")
    private String name;

    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    private String pushToken;
}