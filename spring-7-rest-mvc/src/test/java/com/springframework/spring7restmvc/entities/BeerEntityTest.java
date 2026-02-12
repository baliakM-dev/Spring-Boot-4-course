package com.springframework.spring7restmvc.entities;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class BeerEntityTest {
    private Beer beer() {
        return Beer.builder()
                .beerName("Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("123")
                .price(new BigDecimal("2.50"))
                .quantityOnHand(10)
                .build();
    }

    private Category category(String desc) {
        return Category.builder()
                .description(desc)
                .build();
    }
    @Test
    void addCategory_shouldMaintainBidirectionalRelationship() {
        // Given
        Beer beer = beer();
        Category cat = category("Lager");

        // When
        beer.addCategory(cat);

        // Then
        assertThat(beer.getCategories()).contains(cat);
        assertThat(cat.getBeers()).contains(beer);
    }

    @Test
    void removeCategory_shouldMaintainBidirectionalRelationship() {
        // Given
        Beer beer = beer();
        Category cat = category("IPA");

        beer.addCategory(cat);
        assertThat(beer.getCategories()).contains(cat);
        assertThat(cat.getBeers()).contains(beer);

        // When
        beer.removeCategory(cat);

        // Then
        assertThat(beer.getCategories()).doesNotContain(cat);
        assertThat(cat.getBeers()).doesNotContain(beer);
    }

    @Test
    void addCategory_twice_shouldNotDuplicate() {
        // Given
        Beer beer = beer();
        Category cat = category("Pilsner");

        // When
        beer.addCategory(cat);
        beer.addCategory(cat);

        // Then (Set musí držať unikátnosť)
        assertThat(beer.getCategories()).hasSize(1);
        assertThat(cat.getBeers()).hasSize(1);
    }
}