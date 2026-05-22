package com.dway.dwaybackend.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Icon must not be blank")
    private String icon;

    @NotBlank(message = "Color must not be blank")
    private String color;
}