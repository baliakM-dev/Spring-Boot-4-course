package com.springframework.spring7restmvc.services;

import com.springframework.spring7restmvc.dto.category.CategoryRequestDTO;
import com.springframework.spring7restmvc.dto.category.CategoryResponseDTO;
import com.springframework.spring7restmvc.entities.Category;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.CategoryMapper;
import com.springframework.spring7restmvc.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Service layer for managing category business logic.
 * Handles CRUD operations with proper validation and transaction management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Creates a new category.
     *
     * @param dto category creation request
     * @return created category with generated ID and timestamps
     * @throws ResourceAlreadyExistsExceptions if description exists (case-insensitive)
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO dto) {
        log.debug("Creating new category with description: {}", dto.description());

        // Validate uniqueness
        if (categoryRepository.existsByDescriptionIgnoreCase(dto.description())) {
            log.warn("Category description already exists: {}", dto.description());
            throw new ResourceAlreadyExistsExceptions("Category", "description", dto.description());
        }

        Category category = categoryMapper.dtoToCategory(dto);
        Category saved = categoryRepository.save(category);

        log.info("Created category: id={}, description={}", saved.getId(), saved.getDescription());
        return categoryMapper.categoryToResponseDTO(saved);
    }

    /**
     * Retrieves a category by ID.
     *
     * @param id category ID
     * @return category data
     * @throws NotFoundException if category not found
     */
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(UUID id) {
        log.debug("Fetching category with ID: {}", id);
        Category category = getCategoryOrThrow(id);
        return categoryMapper.categoryToResponseDTO(category);
    }

    /**
     * Retrieves all categories with optional filtering and pagination.
     *
     * @param description optional description filter
     * @param pageable pagination parameters
     * @return page of categories
     */
    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> getAllCategories(String description, Pageable pageable) {
        log.debug("Fetching all categories");

        Page<Category> categoryPage;
        if (StringUtils.hasText(description)) {
            categoryPage = categoryRepository.findAllByDescriptionContainingIgnoreCase(description, pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }

        return categoryPage.map(categoryMapper::categoryToResponseDTO);
    }

    /**
     * Updates an existing category.
     *
     * @param categoryId category ID to update
     * @param dto new category data
     * @return updated category
     * @throws NotFoundException if category not found
     * @throws ResourceAlreadyExistsExceptions if new description conflicts
     */
    @Transactional
    public CategoryResponseDTO updateCategory(UUID categoryId, CategoryRequestDTO dto) {
        log.debug("Updating category with ID: {}", categoryId);

        Category category = getCategoryOrThrow(categoryId);

        // Only validate description uniqueness if it's being changed
        if (!category.getDescription().equalsIgnoreCase(dto.description())) {
            if (categoryRepository.existsByDescriptionIgnoreCaseAndIdNot(dto.description(), categoryId)) {
                log.warn("Category description already exists: {}", dto.description());
                throw new ResourceAlreadyExistsExceptions("Category", "description", dto.description());
            }
        }

        categoryMapper.updateCategoryFromDto(dto, category);

        log.info("Updated category: id={}, description={}", category.getId(), category.getDescription());
        return categoryMapper.categoryToResponseDTO(category);
    }

    /**
     * Deletes a category by ID.
     * Note: Due to CASCADE in join table, this will remove all beer-category associations.
     *
     * @param categoryId category ID to delete
     * @throws NotFoundException if category not found
     */
    @Transactional
    public void deleteCategory(UUID categoryId) {
        log.debug("Deleting category with ID: {}", categoryId);

        Category category = getCategoryOrThrow(categoryId);

        // Clear bidirectional associations before deletion
        category.getBeers().forEach(beer -> beer.getCategories().remove(category));
        category.getBeers().clear();

        categoryRepository.delete(category);

        log.info("Deleted category: id={}", categoryId);
    }

    /**
     * Retrieves a category or throws NotFoundException.
     * Package-private for use by BeerService.
     *
     * @param categoryId category ID
     * @return category entity
     * @throws NotFoundException if not found
     */
    Category getCategoryOrThrow(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found with ID: {}", categoryId);
                    return new NotFoundException("Category", "id", categoryId.toString());
                });
    }
}