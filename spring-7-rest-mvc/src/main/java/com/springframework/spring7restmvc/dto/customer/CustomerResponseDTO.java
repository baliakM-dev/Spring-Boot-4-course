package com.springframework.spring7restmvc.dto.customer;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for customer data.
 * @param id
 * @param name
 * @param createdAt
 * @param updatedAt
 */
public record CustomerResponseDTO(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
)
{ }
