package com.springframework.spring7restmvc.controllers;

import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.services.BeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * Rest controller for beer category management.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BeerCategoryController {

    private final BeerService beerService;

    public static final String BASE_URL = "/api/v1/beers";
    public static final String ADD_REMOVE_CATEGORIES_UDL = BASE_URL + "/{beerId}/categories/{categoryId}";
    public static final String SET_GET_CATEGORIES_URL = BASE_URL + "/{beerId}/categories";

    /**
     * Add a category to a beer.
     * Maintains bidirectional relationship consistency.
     *
     * @param beerId beer UUID
     * @param categoryId category UUID
     * @return updated beer with the new category
     */
    @PostMapping(ADD_REMOVE_CATEGORIES_UDL)
    public ResponseEntity<BeerResponseDTO> addCategoryToBeer(
            @PathVariable UUID beerId,
            @PathVariable UUID categoryId) {
        log.debug("POST /api/v1/beers/{}/categories/{}", beerId, categoryId);
        BeerResponseDTO response = beerService.addCategoryToBeer(beerId, categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a category from a beer.
     * Maintains bidirectional relationship consistency.
     *
     * @param beerId beer UUID
     * @param categoryId category UUID
     * @return updated beer without the category
     */
    @DeleteMapping(ADD_REMOVE_CATEGORIES_UDL)
    public ResponseEntity<BeerResponseDTO> removeCategoryFromBeer(
            @PathVariable UUID beerId,
            @PathVariable UUID categoryId) {
        log.debug("DELETE /api/v1/beers/{}/categories/{}", beerId, categoryId);
        BeerResponseDTO response = beerService.removeCategoryFromBeer(beerId, categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Set all categories for a beer (replaces existing categories).
     * Useful for bulk updates.
     *
     * @param beerId beer UUID
     * @param categoryIds set of category UUIDs to assign
     * @return updated beer with new categories
     */
    @PutMapping(SET_GET_CATEGORIES_URL)
    public ResponseEntity<BeerResponseDTO> setCategoriesForBeer(
            @PathVariable UUID beerId,
            @RequestBody Set<UUID> categoryIds) {
        log.debug("PUT /api/v1/beers/{}/categories - categoryIds: {}", beerId, categoryIds);
        BeerResponseDTO response = beerService.setCategoriesForBeer(beerId, categoryIds);
        return ResponseEntity.ok(response);
    }
}