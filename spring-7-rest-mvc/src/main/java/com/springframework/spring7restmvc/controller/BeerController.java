package com.springframework.spring7restmvc.controller;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.services.BeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing beer resources.
 *
 * <p>This controller provides HTTP endpoints for CRUD operations on beer entities.
 * All endpoints follow RESTful conventions and return appropriate HTTP status codes.</p>
 *
 * <p>Base path: {@code /api/v1/beer}</p>
 *
 * @author Martin Baliak
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class BeerController {

    public static final String BASE_URL = "/api/v1/beer";
    public static final String BASE_URL_ID = BASE_URL + "/{beerId}";

    private final BeerService beerService;

    /**
     * Creates a new beer resource.
     *
     * <p>Validates the request body and ensures beer name uniqueness.</p>
     *
     * @param beer the beer creation request, validated against {@link BeerRequestDTO}
     * @return HTTP 201 Created with Location header pointing to the created beer
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions if name exists
     */
    @PostMapping(BASE_URL)
    public ResponseEntity<Void> createBeer(@Validated @RequestBody BeerRequestDTO beer) {
        log.info("Creating new beer: {}", beer);
        var savedBeer = beerService.createNewBeer(beer);
        return ResponseEntity.created(URI.create(BASE_URL + "/" + savedBeer.id())).build();
    }

    /**
     * Retrieves a specific beer by its unique identifier.
     *
     * @param beerId the unique identifier of the beer
     * @return HTTP 200 OK with beer data
     * @throws com.springframework.spring7restmvc.exceptions.NotFoundException if beer not found
     */
    @GetMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> getBeerById(@PathVariable UUID beerId) {
        log.info("Retrieving beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.getBeerById(beerId));
    }

    /**
     * Retrieves all beers from the system.
     *
     * @return HTTP 200 OK with a list of all beers, empty list if none exist
     */
    @GetMapping(BASE_URL)
    public ResponseEntity<List<BeerResponseDTO>> getAllBeers() {
        log.info("Retrieving all beers");
        return ResponseEntity.ok(beerService.getAllBeers());
    }

    /**
     * Updates an existing beer resource.
     *
     * @param beerId the unique identifier of the beer to update
     * @param beer the updated beer data, validated against {@link BeerRequestDTO}
     * @return HTTP 200 OK with updated beer data
     * @throws com.springframework.spring7restmvc.exceptions.NotFoundException if beer not found
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions if name exists
     */
    @PutMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> updateBeerById(@PathVariable UUID beerId,@Validated @RequestBody BeerRequestDTO beer) {
        log.info("Updating beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.updateBeerById(beerId, beer));
    }

    /**
     * Updates a specific beer field by its unique identifier.
     *
     * @param beerId the unique identifier of the beer to update
     * @param patch the partial beer update, containing only the fields to update
     * @return HTTP 200 OK with updated beer data
     * @throws com.springframework.spring7restmvc.exceptions.NotFoundException if beer not found
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions if name exists
     */
    @PatchMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> patchBeerById(@PathVariable UUID beerId, @RequestBody BeerRequestDTO patch) {
        log.info("Patching beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.patchBeerById(beerId, patch));
    }

    /**
     * Deletes a specific beer by its unique identifier.
     *
     * @param beerId the unique identifier of the beer to delete
     * @return HTTP 204 No Content
     * @throws com.springframework.spring7restmvc.exceptions.NotFoundException if beer not found
     */
    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteBeerById(@PathVariable UUID beerId) {
        log.info("Deleting beer with ID: {}", beerId);
        beerService.deleteBeerById(beerId);
        return ResponseEntity.noContent().build();
    }
}