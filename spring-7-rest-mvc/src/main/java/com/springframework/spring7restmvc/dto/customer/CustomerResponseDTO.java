package com.springframework.spring7restmvc.dto.customer;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponseDTO(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
)
{ }
