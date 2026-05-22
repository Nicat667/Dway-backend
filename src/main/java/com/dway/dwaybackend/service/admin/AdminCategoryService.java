package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.AccessDeniedException;
import com.dway.dwaybackend.common.exception.category.CategoryNameExistsException;
import com.dway.dwaybackend.common.exception.category.CategoryNotFoundException;
import com.dway.dwaybackend.dto.request.category.CreateCategoryRequest;
import com.dway.dwaybackend.dto.request.category.UpdateCategoryRequest;
import com.dway.dwaybackend.dto.response.category.CategoryResponse;
import com.dway.dwaybackend.entity.Category;
import com.dway.dwaybackend.mapper.CategoryMapper;
import com.dway.dwaybackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllDefaultCategories() {
        return categoryMapper.toResponseList(categoryRepository.findByUserIdIsNull());
    }

    @Transactional
    public CategoryResponse createDefaultCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameAndUserId(request.getName(), null)) {
            throw new CategoryNameExistsException();
        }
        Category category = categoryMapper.toEntity(request);
        category.setUserId(null);
        category.setDefault(true);
        categoryRepository.save(category);
        log.info("Admin created default category '{}'", category.getName());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse updateDefaultCategory(UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);

        if (!category.isDefault()) {
            throw new AccessDeniedException();
        }

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByNameAndUserId(request.getName(), null)) {
                throw new CategoryNameExistsException();
            }
            category.setName(request.getName());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }

        categoryRepository.save(category);
        log.info("Admin updated category {}", categoryId);
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void deleteDefaultCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(CategoryNotFoundException::new);

        if (!category.isDefault()) {
            throw new AccessDeniedException();
        }

        categoryRepository.delete(category);
        log.info("Admin deleted category {}", categoryId);
    }
}