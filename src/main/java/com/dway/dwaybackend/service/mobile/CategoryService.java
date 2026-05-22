package com.dway.dwaybackend.service.mobile;

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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(UUID userId) {
        return categoryMapper.toResponseList(categoryRepository.findAllForUser(userId));
    }

    @Transactional
    public CategoryResponse createCategory(UUID userId, CreateCategoryRequest request) {
        if (categoryRepository.existsByNameAndUserId(request.getName(), userId)) {
            throw new CategoryNameExistsException();
        }
        Category category = categoryMapper.toEntity(request);
        category.setUserId(userId);
        categoryRepository.save(category);
        log.info("User {} created category '{}'", userId, category.getName());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = findOwnedCategory(userId, categoryId);

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByNameAndUserId(request.getName(), userId)) {
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
        log.info("User {} updated category {}", userId, categoryId);
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        Category category = findOwnedCategory(userId, categoryId);
        categoryRepository.delete(category);
        log.info("User {} deleted category {}", userId, categoryId);
    }

    // Finds a category by id and enforces ownership:
    // - CategoryNotFoundException if not found
    // - AccessDeniedException if it is a default category or belongs to another user
    private Category findOwnedCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
        if (category.isDefault() || !userId.equals(category.getUserId())) {
            throw new AccessDeniedException();
        }
        return category;
    }
}