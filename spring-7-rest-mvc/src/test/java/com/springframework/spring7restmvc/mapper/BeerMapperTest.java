package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BeerMapperTest {

    private final BeerMapper beerMapper = Mappers.getMapper(BeerMapper.class);

    @Test
    void shouldMapEntityToResponseDTO() {
        Beer beer = Beer.builder()
                .beerName("IPA")
                .beerStyle(BeerStyle.LAGER)
                .upc("123")
                .quantityOnHand(5)
                .price(BigDecimal.TEN)
                .build();

        BeerResponseDTO dto = beerMapper.beerToResponseDTO(beer);

        assertThat(dto.beerName()).isEqualTo("IPA");
        assertThat(dto.beerStyle()).isEqualTo("LAGER");
        assertThat(dto.price()).isEqualTo(BigDecimal.TEN);
    }
}