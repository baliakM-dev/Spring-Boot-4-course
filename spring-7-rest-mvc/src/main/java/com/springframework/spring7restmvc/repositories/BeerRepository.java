package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Beer entity data access.
 * Uses JOIN FETCH and EntityGraph to prevent N+1 problems with categories.
 */
public interface BeerRepository extends JpaRepository<Beer, UUID> {

    /**
     * Find beer by ID with eagerly fetched categories.
     * Prevents N+1 problem when accessing beer's categories.
     *
     * @param id beer ID
     * @return Optional containing beer with categories, or empty
     */
    @Query("SELECT b FROM Beer b LEFT JOIN FETCH b.categories WHERE b.id = :id")
    Optional<Beer> findByIdWithCategories(@Param("id") UUID id);

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
     * @param id       the beer ID to exclude from the check
     * @return true if another beer with this name exists, false otherwise
     */
    boolean existsByBeerNameIgnoreCaseAndIdNot(String beerName, UUID id);

    /**
     * Find all beers by beer name (case-insensitive).
     * Uses EntityGraph to eagerly fetch categories and prevent N+1.
     *
     * @param beerName the beer name to search for
     * @param pageable pagination parameters
     * @return a page of beers matching the name
     */
    @EntityGraph(attributePaths = {"categories"})
    Page<Beer> findAllByBeerNameContainingIgnoreCase(String beerName, Pageable pageable);

    /**
     * Find all beers by style (case-insensitive).+
     * Uses EntityGraph to eagerly fetch categories and prevent N+1.
     *
     * @param beerStyle the beer style to search for
     * @param pageable  pagination parameters
     * @return a page of beers matching the style
     */
    @EntityGraph(attributePaths = {"categories"})
    Page<Beer> findAllByBeerStyle(BeerStyle beerStyle, Pageable pageable);

    /**
     * Find all beers by name and style (case-insensitive).
     * Uses EntityGraph to eagerly fetch categories and prevent N+1.
     *
     * @param beerName  the beer name to search for
     * @param beerStyle the beer style to search for
     * @param pageable  pagination parameters
     * @return a page of beers matching the name and style
     */
    @EntityGraph(attributePaths = {"categories"})
    Page<Beer> findAllByBeerNameContainingIgnoreCaseAndBeerStyle(String beerName, BeerStyle beerStyle, Pageable pageable);

    /**
     * Override default findAll to eagerly fetch categories.
     * Prevents N+1 problem when fetching all beers.
     *
     * @param pageable pagination parameters
     * @return a page of all beers
     */
    @EntityGraph(attributePaths = {"categories"})
    Page<Beer> findAll(Pageable pageable);
}