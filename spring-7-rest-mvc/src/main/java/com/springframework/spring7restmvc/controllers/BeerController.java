package com.springframework.spring7restmvc.controllers;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.services.BeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for beer resource management.
 *
 * Base path: /api/v1/beer
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class BeerController {

    public static final String BASE_URL = "/api/v1/beer";
    public static final String BASE_URL_MANUAL = "/api/v1/beer/manual";
    public static final String BASE_URL_ID = BASE_URL + "/{beerId}";

    private final BeerService beerService;

    @PostMapping(BASE_URL)
    public ResponseEntity<Void> createBeer(@Validated @RequestBody BeerRequestDTO beer) {
        log.info("Creating new beer: {}", beer.beerName());
        var savedBeer = beerService.createNewBeer(beer);
        return ResponseEntity.created(URI.create(BASE_URL + "/" + savedBeer.id())).build();
    }

    @GetMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> getBeerById(@PathVariable UUID beerId) {
        log.debug("Retrieving beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.getBeerById(beerId));
    }

    /**
     * GetAllBeers with spring domain pagination
     */
    @GetMapping(BASE_URL)
    public ResponseEntity<Page<BeerResponseDTO>> getAllBeers(
            @RequestParam(required = false) String beername,
            @RequestParam(required = false) BeerStyle beerStyle,
            @RequestParam(required = false) boolean showInventoryOnHand,
            @PageableDefault(sort = "beerName", direction = Sort.Direction.ASC) Pageable pageable
            ) {
        log.debug("Retrieving all beers");
        return ResponseEntity.ok(beerService.getAllBeers(beername, beerStyle, showInventoryOnHand, pageable));
    }

    /**
     * GetAllBeers with manual pagination
     */
    @GetMapping(BASE_URL_MANUAL)
    public ResponseEntity<List<BeerResponseDTO>> getAllBeersManual(
            @RequestParam(required = false) String beername,
            @RequestParam(required = false) BeerStyle beerStyle,
            @RequestParam(required = false) boolean showInventoryOnHand,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(beerService.getAllBeersManual(beername, beerStyle, showInventoryOnHand, page, size));
    }

    @PutMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> updateBeerById(
            @PathVariable UUID beerId,
            @Validated @RequestBody BeerRequestDTO beer) {
        log.info("Updating beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.updateBeerById(beerId, beer));
    }

    @PatchMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> patchBeerById(
            @PathVariable UUID beerId,
            @RequestBody BeerRequestDTO patch) {
        log.info("Patching beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.patchBeerById(beerId, patch));
    }

    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteBeerById(@PathVariable UUID beerId) {
        log.info("Deleting beer with ID: {}", beerId);
        beerService.deleteBeerById(beerId);
        return ResponseEntity.noContent().build();
    }
}