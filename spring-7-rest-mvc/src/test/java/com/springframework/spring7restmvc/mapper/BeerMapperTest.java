package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BeerMapperTest {

    private final BeerMapper beerMapper = Mappers.getMapper(BeerMapper.class);

    @Test
    void dtoToBeer_shouldIgnoreManagedFieldsAndCategories() {
        // Given
        BeerRequestDTO dto = new BeerRequestDTO(
                "Test Beer",
                BeerStyle.LAGER,
                "123456",
                100,
                new BigDecimal("2.50"),
                Set.of(UUID.randomUUID())
        );

        // When
        Beer beer = beerMapper.dtoToBeer(dto);

        // Then - mapped fields
        assertThat(beer.getBeerName()).isEqualTo("Test Beer");
        assertThat(beer.getBeerStyle()).isEqualTo(BeerStyle.LAGER);
        assertThat(beer.getUpc()).isEqualTo("123456");
        assertThat(beer.getQuantityOnHand()).isEqualTo(100);
        assertThat(beer.getPrice()).isEqualByComparingTo("2.50");

        // Then - ignored managed fields
        assertThat(beer.getId()).isNull();
        assertThat(beer.getVersion()).isNull();
        assertThat(beer.getCreatedAt()).isNull();
        assertThat(beer.getUpdatedAt()).isNull();

        // categories sa ignorujú
        assertThat(beer.getCategories()).isEmpty();
    }

    @Test
    void updateBeerFromDto_shouldUpdateFieldsButKeepManagedFields() {
        // Given
        UUID existingId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime updated = LocalDateTime.now().minusHours(2);

        Beer existing = Beer.builder()
                .id(existingId)
                .beerName("Old")
                .beerStyle(BeerStyle.LAGER)
                .upc("OLDUPC")
                .quantityOnHand(50)
                .price(new BigDecimal("1.00"))
                .createdAt(created)
                .updatedAt(updated)
                .version(7)
                .build();

        BeerRequestDTO dto = new BeerRequestDTO(
                "New Name",
                BeerStyle.IPA,
                "NEWUPC",
                999,
                new BigDecimal("9.99"),
                Set.of(UUID.randomUUID())
        );

        // When
        beerMapper.updateBeerFromDto(dto, existing);

        // Then - updated fields
        assertThat(existing.getBeerName()).isEqualTo("New Name");
        assertThat(existing.getBeerStyle()).isEqualTo(BeerStyle.IPA);
        assertThat(existing.getUpc()).isEqualTo("NEWUPC");
        assertThat(existing.getQuantityOnHand()).isEqualTo(999);
        assertThat(existing.getPrice()).isEqualByComparingTo("9.99");

        // Then - managed fields should remain untouched by mapper
        assertThat(existing.getId()).isEqualTo(existingId);
        assertThat(existing.getCreatedAt()).isEqualTo(created);
        assertThat(existing.getUpdatedAt()).isEqualTo(updated);
        assertThat(existing.getVersion()).isEqualTo(7);

        // categories ignorované
        assertThat(existing.getCategories()).isEmpty();
    }
}