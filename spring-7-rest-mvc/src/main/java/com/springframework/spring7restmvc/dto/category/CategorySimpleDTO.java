package com.springframework.spring7restmvc.dto.category;

import java.util.UUID;

/**
 * Simple DTO for category data.
 * @param id
 * @param description
 */
public record CategorySimpleDTO(
        UUID id,
        String description
) {}
