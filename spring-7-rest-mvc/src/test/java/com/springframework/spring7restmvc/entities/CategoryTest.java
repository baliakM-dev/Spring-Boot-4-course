package com.springframework.spring7restmvc.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for {@link Category}.
 * <p>
 *     Tests for category entity and its relationships.
 * </p>
 */
class CategoryTest {
    private Beer beer() {
        return Beer.builder()
                .beerName("Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("123")
                .price(new BigDecimal("2.50"))
                .quantityOnHand(10)
                .build();
    }

    private Category category() {
        return Category.builder()
                .description("Test Category")
                .build();
    }
    private Category category2() {
        return Category.builder()
                .description("Test Category 2")
                .build();
    }

    @Test
    void add_beer_shouldMaintainBidirectionalRelationship() {
       // Given
        Category cat = category();
        Beer beer = beer();

        // When
        cat.addBeer(beer);

        // Then
        assertThat(cat.getBeers()).contains(beer);
    }

    @Test
    void add_beer_twice_houldNotDuplicate() {
        // Given
        Category cat = category();
        Category cat2 = category2();
        Beer beer = beer();

        // When
        cat.addBeer(beer);
        cat2.addBeer(beer);

        // Then
        assertThat(cat.getBeers()).hasSize(1);
        assertThat(cat2.getBeers()).hasSize(1);
        assertThat(cat.getBeers()).contains(beer);
        assertThat(cat2.getBeers()).contains(beer);
        assertThat(beer.getCategories()).hasSize(2);
    }
}