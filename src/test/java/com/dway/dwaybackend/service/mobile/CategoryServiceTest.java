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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private CategoryService categoryService;

    private static final UUID USER_ID     = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID CATEGORY_ID = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");

    private Category userCategory() {
        return Category.builder()
                .id(CATEGORY_ID)
                .userId(USER_ID)
                .name("Work")
                .icon("briefcase")
                .color("#FF5733")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Category defaultCategory() {
        return Category.builder()
                .id(UUID.randomUUID())
                .userId(null)
                .name("General")
                .icon("star")
                .color("#FFFFFF")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CategoryResponse categoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .name(c.getName())
                .icon(c.getIcon())
                .color(c.getColor())
                .isDefault(c.isDefault())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private CreateCategoryRequest createRequest() {
        CreateCategoryRequest r = new CreateCategoryRequest();
        r.setName("Work");
        r.setIcon("briefcase");
        r.setColor("#FF5733");
        return r;
    }

    private UpdateCategoryRequest updateRequest() {
        UpdateCategoryRequest r = new UpdateCategoryRequest();
        r.setName("Updated Work");
        r.setIcon("laptop");
        r.setColor("#00FF00");
        return r;
    }

    // ══════════════════════════════════════════════════════════════
    // getAllCategories
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("returns mix of default and own categories")
        void withExistingCategories_returnsAll() {
            List<Category> categories = List.of(defaultCategory(), userCategory());
            List<CategoryResponse> responses = categories.stream()
                    .map(CategoryServiceTest.this::categoryResponse).toList();

            when(categoryRepository.findAllForUser(USER_ID)).thenReturn(categories);
            when(categoryMapper.toResponseList(categories)).thenReturn(responses);

            List<CategoryResponse> result = categoryService.getAllCategories(USER_ID);

            assertThat(result).hasSize(2);
            verify(categoryRepository).findAllForUser(USER_ID);
        }

        @Test
        @DisplayName("returns empty list when user has no categories")
        void whenNoCategories_returnsEmptyList() {
            when(categoryRepository.findAllForUser(USER_ID)).thenReturn(Collections.emptyList());
            when(categoryMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<CategoryResponse> result = categoryService.getAllCategories(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("passes userId to repository without modification")
        void always_passesUserIdToRepository() {
            when(categoryRepository.findAllForUser(USER_ID)).thenReturn(Collections.emptyList());
            when(categoryMapper.toResponseList(any())).thenReturn(Collections.emptyList());

            categoryService.getAllCategories(USER_ID);

            verify(categoryRepository).findAllForUser(USER_ID);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // createCategory
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {

        @Test
        @DisplayName("saves category with correct userId and returns response")
        void withValidRequest_savesAndReturnsResponse() {
            Category entity = userCategory();
            CategoryResponse response = categoryResponse(entity);
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), USER_ID)).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(entity);
            when(categoryMapper.toResponse(entity)).thenReturn(response);

            CategoryResponse result = categoryService.createCategory(USER_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CATEGORY_ID);
            verify(categoryRepository).save(entity);
        }

        @Test
        @DisplayName("sets userId from parameter onto the entity")
        void always_setsUserIdFromParameter() {
            Category entity = userCategory();
            entity.setUserId(null);
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), USER_ID)).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(entity);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.createCategory(USER_ID, request);

            assertThat(entity.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws CategoryNameExistsException when name is already taken by this user")
        void withDuplicateName_throwsCategoryNameExistsException() {
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), USER_ID)).thenReturn(true);

            assertThrows(CategoryNameExistsException.class,
                    () -> categoryService.createCategory(USER_ID, request));

            verify(categoryRepository, never()).save(any());
            verify(categoryMapper, never()).toEntity(any());
        }

        @Test
        @DisplayName("checks name uniqueness before saving")
        void always_checksNameUniquenessFirst() {
            Category entity = userCategory();
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), USER_ID)).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(entity);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.createCategory(USER_ID, request);

            InOrder order = inOrder(categoryRepository);
            order.verify(categoryRepository).existsByNameAndUserId(request.getName(), USER_ID);
            order.verify(categoryRepository).save(entity);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // updateCategory
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategory {

        @Test
        @DisplayName("updates all non-null fields when all provided")
        void withAllFields_updatesAll() {
            Category entity = userCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryRepository.existsByNameAndUserId("Updated Work", USER_ID)).thenReturn(false);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, updateRequest());

            assertThat(entity.getName()).isEqualTo("Updated Work");
            assertThat(entity.getIcon()).isEqualTo("laptop");
            assertThat(entity.getColor()).isEqualTo("#00FF00");
            verify(categoryRepository).save(entity);
        }

        @Test
        @DisplayName("empty request leaves all fields unchanged")
        void withEmptyRequest_noFieldsModified() {
            Category entity = userCategory(); // name=Work, icon=briefcase, color=#FF5733

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, new UpdateCategoryRequest());

            assertThat(entity.getName()).isEqualTo("Work");
            assertThat(entity.getIcon()).isEqualTo("briefcase");
            assertThat(entity.getColor()).isEqualTo("#FF5733");
        }

        @Test
        @DisplayName("does not update name when name is null")
        void withNullName_nameUnchanged() {
            Category entity = userCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setName(null);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, request);

            assertThat(entity.getName()).isEqualTo("Work");
            verify(categoryRepository, never()).existsByNameAndUserId(any(), any());
        }

        @Test
        @DisplayName("does not check name uniqueness when name is unchanged")
        void withSameName_skipsUniquenessCheck() {
            Category entity = userCategory(); // name = "Work"
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setName("Work"); // same as current

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, request);

            verify(categoryRepository, never()).existsByNameAndUserId(any(), any());
        }

        @Test
        @DisplayName("does not update icon when icon is null")
        void withNullIcon_iconUnchanged() {
            Category entity = userCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setIcon(null);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, request);

            assertThat(entity.getIcon()).isEqualTo("briefcase");
        }

        @Test
        @DisplayName("does not update color when color is null")
        void withNullColor_colorUnchanged() {
            Category entity = userCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setColor(null);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, request);

            assertThat(entity.getColor()).isEqualTo("#FF5733");
        }

        @Test
        @DisplayName("throws CategoryNameExistsException when new name is already taken")
        void withDuplicateNewName_throwsCategoryNameExistsException() {
            Category entity = userCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setName("Personal"); // different from "Work", but already taken

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryRepository.existsByNameAndUserId("Personal", USER_ID)).thenReturn(true);

            assertThrows(CategoryNameExistsException.class,
                    () -> categoryService.updateCategory(USER_ID, CATEGORY_ID, request));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when category does not exist")
        void whenCategoryNotFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThrows(CategoryNotFoundException.class,
                    () -> categoryService.updateCategory(USER_ID, CATEGORY_ID, updateRequest()));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when category belongs to different user")
        void whenCategoryBelongsToDifferentUser_throwsAccessDeniedException() {
            Category entity = userCategory();
            entity.setUserId(UUID.randomUUID());

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> categoryService.updateCategory(USER_ID, CATEGORY_ID, updateRequest()));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when category is a default system category")
        void whenCategoryIsDefault_throwsAccessDeniedException() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> categoryService.updateCategory(USER_ID, entity.getId(), updateRequest()));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves category after applying updates")
        void always_savesAfterUpdate() {
            Category entity = userCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            categoryService.updateCategory(USER_ID, CATEGORY_ID, new UpdateCategoryRequest());

            verify(categoryRepository).save(entity);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // deleteCategory
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategory {

        @Test
        @DisplayName("physically deletes the category record")
        void withValidCategory_callsRepositoryDelete() {
            Category entity = userCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

            categoryService.deleteCategory(USER_ID, CATEGORY_ID);

            verify(categoryRepository).delete(entity);
        }

        @Test
        @DisplayName("does not use soft-delete (no isDeleted flag on Category)")
        void withValidCategory_doesNotCallSave() {
            Category entity = userCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

            categoryService.deleteCategory(USER_ID, CATEGORY_ID);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when category does not exist")
        void whenCategoryNotFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThrows(CategoryNotFoundException.class,
                    () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when category belongs to different user")
        void whenCategoryBelongsToDifferentUser_throwsAccessDeniedException() {
            Category entity = userCategory();
            entity.setUserId(UUID.randomUUID());

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when category is a default system category")
        void whenCategoryIsDefault_throwsAccessDeniedException() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> categoryService.deleteCategory(USER_ID, entity.getId()));

            verify(categoryRepository, never()).delete(any());
        }
    }
}