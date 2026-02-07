package com.springframework.spring7restmvc.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BeerTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void priceMustBePositive() {
        Beer beer = Beer.builder()
                .beerName("Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("1234567890123")
                .price(BigDecimal.valueOf(-10)) // <- záporná cena
                .build();

        var violations = validator.validate(beer);

        assertThat(violations)
                .anyMatch(v ->
                        v.getPropertyPath().toString().equals("price")
                );
    }

    @Test
    void validBeerHasNoViolations() {
        Beer beer = Beer.builder()
                .beerName("Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("1234567890123")
                .price(BigDecimal.valueOf(10))
                .build();

        var violations = validator.validate(beer);

        assertThat(violations).isEmpty();
    }

    @Test
    void createBeer() {
        Beer beer = Beer.builder()
                .beerName("Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("1234567890123")
                .price(BigDecimal.valueOf(10))
                .build();
        assertThat(beer).isNotNull();
    }
}