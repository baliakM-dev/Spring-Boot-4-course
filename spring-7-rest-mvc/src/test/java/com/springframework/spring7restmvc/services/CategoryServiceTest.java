package com.springframework.spring7restmvc.services;

import com.github.dockerjava.api.exception.ConflictException;
import com.springframework.spring7restmvc.dto.category.CategoryRequestDTO;
import com.springframework.spring7restmvc.dto.category.CategoryResponseDTO;
import com.springframework.spring7restmvc.entities.Category;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.CategoryMapper;
import com.springframework.spring7restmvc.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CategoryService}.
 * <p>
 * Focus:
 * - uniqueness validation
 * - not found / conflict exceptions
 * <p>
 * Note: These are pure unit tests (no Spring, no DB).
 */
@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    CategoryMapper categoryMapper;
    @InjectMocks
    CategoryService categoryService;

    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
    }

    // ==================== Fixtures ====================
    private CategoryRequestDTO dto(String description) {
        return new CategoryRequestDTO(description);
    }

    private Category category(UUID id, String description) {
        return Category.builder().id(id).description(description).build();
    }

    private CategoryResponseDTO responseDto(String description) {
        return new CategoryResponseDTO(categoryId, null, description, null, null);
    }

    private Page<Category> pageOf(Category... category) {
        return new PageImpl<>(List.of(category), PageRequest.of(0, 10), category.length);
    }

    // ==================== Create ====================
    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        void should_CreateCategory_whenNameIsUnique() {
            // Given
            CategoryRequestDTO request = dto("Lager");

            Category mapped = category(null, "Lager");
            Category saved = category(categoryId, "Lager");
            CategoryResponseDTO response = responseDto("Lager");

            when(categoryRepository.existsByDescriptionIgnoreCase("Lager")).thenReturn(false);
            when(categoryMapper.dtoToCategory(request)).thenReturn(mapped);
            when(categoryRepository.save(mapped)).thenReturn(saved);
            when(categoryMapper.categoryToResponseDTO(saved)).thenReturn(response);

            // When
            CategoryResponseDTO result = categoryService.createCategory(request);

            // Then
            assertThat(result).isEqualTo(response);
            assertThat(result.id()).isEqualTo(categoryId);
            assertThat(result.description()).isEqualTo("Lager");

            verify(categoryRepository).save(mapped);
            verify(categoryMapper).categoryToResponseDTO(saved);
            verify(categoryRepository).existsByDescriptionIgnoreCase("Lager");
            verify(categoryMapper).dtoToCategory(request);
        }

        @Test
        void shouldThrowConflict_whenNameChangedAndDuplicateExists() {
            // Given
            CategoryRequestDTO request = dto("Lager");
            when(categoryRepository.existsByDescriptionIgnoreCase("Lager")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceAlreadyExistsExceptions.class);

            verify(categoryRepository).existsByDescriptionIgnoreCase("Lager");
            verifyNoMoreInteractions(categoryRepository);
        }

        // ==================== Get By Id ====================
        @Nested
        @DisplayName("Get Category By Id Tests")
        class GetCategoryByIdTests {

            @Test
            void should_ReturnCategory_whenIdExists() {
                // Given
                Category found = category(categoryId, "Lager");
                CategoryResponseDTO responseDto = responseDto("Lager");

                when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(found));
                when(categoryMapper.categoryToResponseDTO(found)).thenReturn(responseDto);

                // When
                CategoryResponseDTO result = categoryService.getCategoryById(categoryId);

                // Then
                assertThat(result.id()).isEqualTo(categoryId);
                assertThat(result.description()).isEqualTo("Lager");

                verify(categoryRepository).findById(categoryId);
                verify(categoryMapper).categoryToResponseDTO(found);
                verifyNoMoreInteractions(categoryRepository, categoryMapper);
            }

            @Test
            void should_ThrowNotFound_whenMissing() {
                // Given
                when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

                // When/Then
                assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                    .isInstanceOf(NotFoundException.class);

                verify(categoryRepository).findById(categoryId);
                verifyNoMoreInteractions(categoryRepository, categoryMapper);
            }
        }

        // ==================== Update ====================

        @Nested
        @DisplayName("Update Category Tests")
        class UpdateCategoryTests {

            @Test
            void should_UpdateCategory_whenNameChangedAndUnique() {
                // Given
                Category existing = category(categoryId, "Old");
                CategoryRequestDTO request = dto("New");

                when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
                when(categoryRepository.existsByDescriptionIgnoreCaseAndIdNot("New", categoryId)).thenReturn(false);

                // MapStruct update: zmutuje entity
                doAnswer(invocation -> {
                    CategoryRequestDTO req = invocation.getArgument(0);
                    Category target = invocation.getArgument(1);
                    target.setDescription(req.description());
                    return null;
                }).when(categoryMapper).updateCategoryFromDto(request, existing);

                // mapper response musí vrátiť "New"
                when(categoryMapper.categoryToResponseDTO(existing)).thenReturn(responseDto("New"));

                // When
                CategoryResponseDTO result = categoryService.updateCategory(categoryId, request);

                // Then
                assertThat(result.description()).isEqualTo("New");

                verify(categoryRepository).findById(categoryId);
                verify(categoryRepository).existsByDescriptionIgnoreCaseAndIdNot("New", categoryId);
                verify(categoryMapper).updateCategoryFromDto(request, existing);
                verify(categoryMapper).categoryToResponseDTO(existing);
                verifyNoMoreInteractions(categoryRepository, categoryMapper);
            }
        }

    }

}
