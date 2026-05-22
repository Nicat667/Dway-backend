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
@DisplayName("AdminCategoryService Unit Tests")
class AdminCategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private AdminCategoryService adminCategoryService;

    private static final UUID CATEGORY_ID      = UUID.fromString("c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0");
    private static final UUID USER_CATEGORY_ID = UUID.fromString("d0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d0d0");
    private static final UUID USER_ID          = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");

    private Category defaultCategory() {
        return Category.builder()
                .id(CATEGORY_ID)
                .userId(null)
                .name("General")
                .icon("star")
                .color("#FFFFFF")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Category userCategory() {
        return Category.builder()
                .id(USER_CATEGORY_ID)
                .userId(USER_ID)
                .name("Work")
                .icon("briefcase")
                .color("#FF5733")
                .isDefault(false)
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
        r.setName("General");
        r.setIcon("star");
        r.setColor("#FFFFFF");
        return r;
    }

    private UpdateCategoryRequest updateRequest() {
        UpdateCategoryRequest r = new UpdateCategoryRequest();
        r.setName("Updated");
        r.setIcon("flame");
        r.setColor("#000000");
        return r;
    }

    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("returns only default categories")
        void returnsOnlyDefaultCategories() {
            Category def = defaultCategory();
            List<Category> categories = List.of(def);
            List<CategoryResponse> responses = List.of(categoryResponse(def));

            when(categoryRepository.findByUserIdIsNull()).thenReturn(categories);
            when(categoryMapper.toResponseList(categories)).thenReturn(responses);

            List<CategoryResponse> result = adminCategoryService.getAllDefaultCategories();

            assertThat(result).hasSize(1);
            verify(categoryRepository).findByUserIdIsNull();
        }

        @Test
        @DisplayName("returns empty list when no default categories exist")
        void whenNoDefaults_returnsEmptyList() {
            when(categoryRepository.findByUserIdIsNull()).thenReturn(Collections.emptyList());
            when(categoryMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<CategoryResponse> result = adminCategoryService.getAllDefaultCategories();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createDefaultCategory()")
    class CreateDefaultCategory {

        @Test
        @DisplayName("saves category with null userId and isDefault=true")
        void withValidRequest_savesDefaultCategory() {
            Category entity = defaultCategory();
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), null)).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(entity);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            CategoryResponse result = adminCategoryService.createDefaultCategory(request);

            assertThat(result).isNotNull();
            assertThat(entity.getUserId()).isNull();
            assertThat(entity.isDefault()).isTrue();
            verify(categoryRepository).save(entity);
        }

        @Test
        @DisplayName("sets isDefault to true regardless of mapper output")
        void always_setsIsDefaultTrue() {
            Category entity = defaultCategory();
            entity.setDefault(false);
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), null)).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(entity);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.createDefaultCategory(request);

            assertThat(entity.isDefault()).isTrue();
        }

        @Test
        @DisplayName("checks uniqueness within default namespace (null userId)")
        void always_checksNullUserIdNamespace() {
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), null)).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(defaultCategory());
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse(defaultCategory()));

            adminCategoryService.createDefaultCategory(request);

            verify(categoryRepository).existsByNameAndUserId(request.getName(), null);
        }

        @Test
        @DisplayName("throws CategoryNameExistsException when default name is already taken")
        void withDuplicateDefaultName_throwsCategoryNameExistsException() {
            CreateCategoryRequest request = createRequest();

            when(categoryRepository.existsByNameAndUserId(request.getName(), null)).thenReturn(true);

            assertThrows(CategoryNameExistsException.class,
                    () -> adminCategoryService.createDefaultCategory(request));

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategory {

        @Test
        @DisplayName("updates all non-null fields on a default category")
        void withAllFields_updatesDefaultCategory() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryRepository.existsByNameAndUserId("Updated", null)).thenReturn(false);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, updateRequest());

            assertThat(entity.getName()).isEqualTo("Updated");
            assertThat(entity.getIcon()).isEqualTo("flame");
            assertThat(entity.getColor()).isEqualTo("#000000");
            verify(categoryRepository).save(entity);
        }

        @Test
        @DisplayName("checks uniqueness within default namespace (null userId)")
        void always_checksNullUserIdNamespace() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryRepository.existsByNameAndUserId("Updated", null)).thenReturn(false);
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, updateRequest());

            verify(categoryRepository).existsByNameAndUserId("Updated", null);
        }

        @Test
        @DisplayName("does not update name when name is null")
        void withNullName_nameUnchanged() {
            Category entity = defaultCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setName(null);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, request);

            assertThat(entity.getName()).isEqualTo("General");
            verify(categoryRepository, never()).existsByNameAndUserId(any(), any());
        }

        @Test
        @DisplayName("does not check uniqueness when name is unchanged")
        void withSameName_skipsUniquenessCheck() {
            Category entity = defaultCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setName("General");

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, request);

            verify(categoryRepository, never()).existsByNameAndUserId(any(), any());
        }

        @Test
        @DisplayName("does not update icon when icon is null")
        void withNullIcon_iconUnchanged() {
            Category entity = defaultCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setIcon(null);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, request);

            assertThat(entity.getIcon()).isEqualTo("star");
        }

        @Test
        @DisplayName("does not update color when color is null")
        void withNullColor_colorUnchanged() {
            Category entity = defaultCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setColor(null);

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, request);

            assertThat(entity.getColor()).isEqualTo("#FFFFFF");
        }

        @Test
        @DisplayName("empty request leaves all fields unchanged")
        void withEmptyRequest_noFieldsModified() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryMapper.toResponse(entity)).thenReturn(categoryResponse(entity));

            adminCategoryService.updateDefaultCategory(CATEGORY_ID, new UpdateCategoryRequest());

            assertThat(entity.getName()).isEqualTo("General");
            assertThat(entity.getIcon()).isEqualTo("star");
            assertThat(entity.getColor()).isEqualTo("#FFFFFF");
        }

        @Test
        @DisplayName("throws CategoryNameExistsException when new name is already taken")
        void withDuplicateName_throwsCategoryNameExistsException() {
            Category entity = defaultCategory();
            UpdateCategoryRequest request = new UpdateCategoryRequest();
            request.setName("Health");

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
            when(categoryRepository.existsByNameAndUserId("Health", null)).thenReturn(true);

            assertThrows(CategoryNameExistsException.class,
                    () -> adminCategoryService.updateDefaultCategory(CATEGORY_ID, request));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when category does not exist")
        void whenCategoryNotFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThrows(CategoryNotFoundException.class,
                    () -> adminCategoryService.updateDefaultCategory(CATEGORY_ID, updateRequest()));

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when category is user-created")
        void whenCategoryIsUserCreated_throwsAccessDeniedException() {
            Category entity = userCategory();

            when(categoryRepository.findById(USER_CATEGORY_ID)).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> adminCategoryService.updateDefaultCategory(USER_CATEGORY_ID, updateRequest()));

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategory {

        @Test
        @DisplayName("deletes a default category")
        void withDefaultCategory_deletesSuccessfully() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

            adminCategoryService.deleteDefaultCategory(CATEGORY_ID);

            verify(categoryRepository).delete(entity);
        }

        @Test
        @DisplayName("does not use soft-delete")
        void withValidCategory_doesNotCallSave() {
            Category entity = defaultCategory();

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));

            adminCategoryService.deleteDefaultCategory(CATEGORY_ID);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when category does not exist")
        void whenCategoryNotFound_throwsCategoryNotFoundException() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThrows(CategoryNotFoundException.class,
                    () -> adminCategoryService.deleteDefaultCategory(CATEGORY_ID));

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when category is user-created")
        void whenCategoryIsUserCreated_throwsAccessDeniedException() {
            Category entity = userCategory();

            when(categoryRepository.findById(USER_CATEGORY_ID)).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> adminCategoryService.deleteDefaultCategory(USER_CATEGORY_ID));

            verify(categoryRepository, never()).delete(any());
        }
    }
}