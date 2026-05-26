package com.dway.dwaybackend.dto.request.user;

import com.dway.dwaybackend.entity.enums.Country;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Size(min = 2, max = 50, message = "Surname must be between 2 and 50 characters")
    private String surname;

    private Country country;

    private String pushToken;
}