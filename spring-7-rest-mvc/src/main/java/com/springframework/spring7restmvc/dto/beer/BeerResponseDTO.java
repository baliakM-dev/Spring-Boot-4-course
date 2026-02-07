package com.springframework.spring7restmvc.dto.beer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BeerResponseDTO(
        UUID id,
        String beerName,
        String beerStyle,
        String upc,
        Integer quantityOnHand,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}
