package com.springframework.spring7restmvc.dto.beer;

import com.springframework.spring7restmvc.entities.BeerStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for beer creation requests.
 *
 * <p>This DTO contains all required information for creating a new beer
 * and includes validation constraints to ensure data integrity.</p>
 *
 * @param beerName the name of the beer, must not be blank
 * @param beerStyle the style/category of the beer, must not be null
 * @param upc the Universal Product Code, must not be blank
 * @param quantityOnHand the current inventory quantity, must be zero or positive
 * @param price the price of the beer, must be greater than zero
 */
public record BeerRequestDTO(

        @NotBlank(message = "{beer.name.notblank}")
        String beerName,

        @NotNull(message = "{beer.beerStyle.notblank}")
        BeerStyle beerStyle,

        @NotBlank(message = "{beer.upc.notblank}")
        String upc,

        @PositiveOrZero(message = "{beer.upc.notblank}")
        Integer quantityOnHand,

        @NotNull(message = "{beer.price.notblank}")
        @Positive(message = "{beer.pricePositive.notblank}")
        BigDecimal price,

        Set<UUID> categoryIds
) {}