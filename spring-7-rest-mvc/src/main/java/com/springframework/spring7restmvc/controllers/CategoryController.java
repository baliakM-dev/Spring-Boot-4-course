package com.springframework.spring7restmvc.controllers;

import com.springframework.spring7restmvc.dto.category.CategoryRequestDTO;
import com.springframework.spring7restmvc.dto.category.CategoryResponseDTO;
import com.springframework.spring7restmvc.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for Category resource management.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CategoryController {

    public static final String BASE_URL = "/api/v1/categories";
    public static final String BASE_URL_ID = BASE_URL + "/{categoryId}";

    private final CategoryService categoryService;

    /**
     * Create a new category.
     *
     * @param dto category creation request
     * @return status code 201 Created and location of created resource
     */
    @PostMapping(BASE_URL)
    public ResponseEntity<CategoryResponseDTO> createCategory(@Validated @RequestBody CategoryRequestDTO dto) {
        log.debug("POST /api/v1/categories - dto: {}", dto);
        CategoryResponseDTO created = categoryService.createCategory(dto);
        return ResponseEntity.created(URI.create(BASE_URL + "/" + created.id())).build();
    }

    /**
     * Get all categories with optional filtering and pagination.
     *
     * @param description optional description filter (case-insensitive contains)
     * @param pageable    pagination parameters (page, size, sort)
     * @return page of categories
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
     */
    @GetMapping(BASE_URL_ID)
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable UUID categoryId) {
        log.debug("GET /api/v1/categories/{}", categoryId);
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId));
    }

    /**
     * Update an existing category.
     *
     * @param categoryId category UUID
     * @param dto        updated category data
     * @return updated category
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
     */
    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        log.debug("DELETE /api/v1/categories/{}", categoryId);
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}