package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Beer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BeerRepository extends JpaRepository<Beer, UUID> {
    boolean existsByBeerNameIgnoreCase(String beerName);
}
