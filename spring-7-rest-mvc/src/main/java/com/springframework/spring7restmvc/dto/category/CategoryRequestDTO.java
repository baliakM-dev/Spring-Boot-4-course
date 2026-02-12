package com.springframework.spring7restmvc.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a category.
 * @param description the category's description
 */
public record CategoryRequestDTO(
        @NotBlank(message = "Description is required")
        @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
        String description
) {
}