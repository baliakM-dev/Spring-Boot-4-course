package com.springframework.spring7restmvc.dto.beer;

import com.springframework.spring7restmvc.dto.category.CategorySimpleDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record BeerResponseDTO(
        UUID id,
        String beerName,
        String beerStyle,
        String upc,
        Integer quantityOnHand,
        BigDecimal price,
        Set<CategorySimpleDTO> categories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}
