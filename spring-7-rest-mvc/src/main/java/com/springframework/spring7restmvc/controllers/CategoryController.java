package com.springframework.spring7restmvc.controllers;

import com.springframework.spring7restmvc.dto.category.CategoryRequestDTO;
import com.springframework.spring7restmvc.dto.category.CategoryResponseDTO;
import com.springframework.spring7restmvc.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Category resource.
 * Provides CRUD operations for beer categories.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CategoryController {

    public static final String BASE_URL = "/api/v1/categories";
    public static final String BASE_URL_ID = BASE_URL + "/{categoryId}";


    private final CategoryService categoryService;

    /**
     * Get all categories with optional filtering and pagination.
     *
     * @param description optional description filter (case-insensitive contains)
     * @param pageable pagination parameters (page, size, sort)
     * @return page of categories
     *
     * Example: GET /api/v1/categories?description=IPA&page=0&size=10&sort=description,asc
     */
    @GetMapping(BASE_URL)
    public ResponseEntity<Page<CategoryResponseDTO>> getAllCategories(
            @RequestParam(required = false) String description,
            Pageable pageable) {
        log.debug("GET /api/v1/categories - description: {}, pageable: {}", description, pageable);
        return ResponseEntity.ok(categoryService.getAllCategories(description, pageable));
    }

    /**
     * Get a specific category by ID.
     *
     * @param categoryId category UUID
     * @return category data
     *
     * Example: GET /api/v1/categories/123e4567-e89b-12d3-a456-426614174000
     */
    @GetMapping(BASE_URL_ID)
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable UUID categoryId) {
        log.debug("GET /api/v1/categories/{}", categoryId);
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId));
    }

    /**
     * Create a new category.
     *
     * @param dto category creation request
     * @return created category with HTTP 201
     *
     * Example:
     * POST /api/v1/categories
     * {
     *   "description": "India Pale Ale"
     * }
     */
    @PostMapping(BASE_URL)
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO dto) {
        log.debug("POST /api/v1/categories - dto: {}", dto);
        CategoryResponseDTO created = categoryService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing category.
     *
     * @param categoryId category UUID
     * @param dto updated category data
     * @return updated category
     *
     * Example:
     * PUT /api/v1/categories/123e4567-e89b-12d3-a456-426614174000
     * {
     *   "description": "American IPA"
     * }
     */
    @PutMapping(BASE_URL_ID)
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequestDTO dto) {
        log.debug("PUT /api/v1/categories/{} - dto: {}", categoryId, dto);
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, dto));
    }

    /**
     * Delete a category.
     * Note: This will remove all beer-category associations due to CASCADE.
     *
     * @param categoryId category UUID
     * @return HTTP 204 No Content
     *
     * Example: DELETE /api/v1/categories/123e4567-e89b-12d3-a456-426614174000
     */
    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        log.debug("DELETE /api/v1/categories/{}", categoryId);
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}