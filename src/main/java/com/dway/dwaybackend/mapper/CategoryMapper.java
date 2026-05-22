package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.request.category.CreateCategoryRequest;
import com.dway.dwaybackend.dto.response.category.CategoryResponse;
import com.dway.dwaybackend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // boolean field named isXxx causes MapStruct property-name ambiguity,
    // so we resolve it explicitly via expression
    @Mapping(target = "isDefault", expression = "java(category.isDefault())")
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);
}