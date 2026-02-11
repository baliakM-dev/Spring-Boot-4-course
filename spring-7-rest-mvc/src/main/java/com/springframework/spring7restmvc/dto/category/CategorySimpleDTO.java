package com.springframework.spring7restmvc.dto.category;

import java.util.UUID;

public record CategorySimpleDTO(
        UUID id,
        String description
) {}
