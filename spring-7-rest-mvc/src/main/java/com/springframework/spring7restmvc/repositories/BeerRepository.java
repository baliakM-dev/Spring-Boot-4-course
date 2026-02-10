package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Beer entity data access.
 */
public interface BeerRepository extends JpaRepository<Beer, UUID> {

    /**
     * Checks if a beer with the given name exists (case-insensitive).
     *
     * @param beerName the beer name to check
     * @return true if beer exists, false otherwise
     */
    boolean existsByBeerNameIgnoreCase(String beerName);

    /**
     * Checks if a beer with the given name exists, excluding a specific beer ID.
     * Useful for update operations where we want to allow keeping the same name.
     *
     * @param beerName the beer name to check
     * @param id the beer ID to exclude from the check
     * @return true if another beer with this name exists, false otherwise
     */
    boolean existsByBeerNameIgnoreCaseAndIdNot(String beerName, UUID id);

    /**
     * Find all beers by beer name (case-insensitive).
     * @param beerName the beer name to search for
     * @param pageable pagination parameters
     * @return a page of beers matching the name
     */
    Page<Beer> findAllByBeerNameContainingIgnoreCase(String beerName, Pageable pageable);

    /**
     * Find all beers by style (case-insensitive).
     *
     * @param beerStyle the beer style to search for
     * @param pageable pagination parameters
     * @return a page of beers matching the style
     */
    Page<Beer> findAllByBeerStyle(BeerStyle beerStyle, Pageable pageable);

    /**
     * Find all beers by name and style (case-insensitive).
     *
     * @param beerName the beer name to search for
     * @param beerStyle the beer style to search for
     * @param pageable pagination parameters
     * @return a page of beers matching the name and style
     */
    Page<Beer> findAllByBeerNameContainingIgnoreCaseAndBeerStyle(String beerName, BeerStyle beerStyle, Pageable pageable);
}