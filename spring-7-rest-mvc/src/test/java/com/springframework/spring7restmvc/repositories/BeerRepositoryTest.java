package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BeerRepositoryTest {

    @Autowired
    BeerRepository beerRepository;

    @Test
    void createBeerJpaTest() {
        Beer beer = Beer.builder()
                .beerName("Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("1234567890123")
                .price(BigDecimal.valueOf(10))
                .build();
        Beer savedBeer = beerRepository.save(beer);
        assertNotNull(savedBeer);
    }

    @Test
    void savedBeerValidationTest() {
        Beer beer = Beer.builder()
                .beerName("Test BeersssssssssssssssssssssssssssTest Beersssssssssssssssssssssssssss")
                .beerStyle(BeerStyle.LAGER)
                .upc("1234567890123")
                .price(BigDecimal.valueOf(10))
                .build();

        assertThrows(ConstraintViolationException.class, () -> {
            beerRepository.saveAndFlush(beer);
        });
    }

    @Test
    void testFindAll() {
        List<Beer> beers = beerRepository.findAll();
        assertNotNull(beers);
        beerRepository.findAll().forEach(System.out::println);
    }
}