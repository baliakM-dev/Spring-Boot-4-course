package com.springframework.spring7restmvc.dto.beer;

import com.springframework.spring7restmvc.dto.category.CategorySimpleDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for beer responses.
 * @param id
 * @param beerName
 * @param beerStyle
 * @param upc
 * @param quantityOnHand
 * @param price
 * @param categories
 * @param createdAt
 * @param updatedAt
 */
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
