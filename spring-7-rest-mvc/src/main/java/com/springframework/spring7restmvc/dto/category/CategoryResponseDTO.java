package com.springframework.spring7restmvc.dto.category;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for category data.
 * Does not include beers collection to avoid circular references and N+1 issues.
 * @param id
 * @param version
 * @param description
 * @param createdAt
 * @param updatedAt
 */
public record CategoryResponseDTO(
        UUID id,
        Integer version,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}