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
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class BeerController {

    public static final String BASE_URL = "/api/v1/beers";
    public static final String BASE_URL_MANUAL = "/api/v1/beers/manual";
    public static final String BASE_URL_ID = BASE_URL + "/{beerId}";

    private final BeerService beerService;

    /**
     * Creates a new beer with or withour categories.
     *
     * @param beerRequestDTO beer creation request
     * @return status code 201 Created and location of created resource
     */
    @PostMapping(BASE_URL)
    public ResponseEntity<Void> createBeer(@Validated @RequestBody BeerRequestDTO beerRequestDTO) {
        log.info("Creating new beer: {}", beerRequestDTO.beerName());
        var savedBeer = beerService.createNewBeer(beerRequestDTO);
        return ResponseEntity.created(URI.create(BASE_URL + "/" + savedBeer.id())).build();
    }

    /**
     * GetAllBeers with spring domain pagination
     *
     * @param pageable            pagination parameters (page, size, sort)
     * @param beerName            optional beer name filter (case-insensitive contains)
     * @param beerStyle           optional beer style filter
     * @param showInventoryOnHand optional flag to include inventory on hand
     * @return paginated list of beers
     */
    @GetMapping(BASE_URL)
    public ResponseEntity<Page<BeerResponseDTO>> getAllBeers(
            @RequestParam(required = false) String beerName,
            @RequestParam(required = false) BeerStyle beerStyle,
            @RequestParam(required = false) boolean showInventoryOnHand,
            @PageableDefault(sort = "beerName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("Retrieving all beers");
        return ResponseEntity.ok(beerService.getAllBeers(beerName, beerStyle, showInventoryOnHand, pageable));
    }

    /**
     * GetAllBeers with manual pagination
     *
     * @param beername            optional beer name filter (case-insensitive contains)
     * @param beerStyle           optional beer style filter
     * @param showInventoryOnHand optional flag to include inventory on hand
     * @param page                page number
     * @param size                page size
     * @return list of beers
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

    /**
     * GetBeerById
     *
     * @param beerId
     * @return BeerResponseDTO
     */
    @GetMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> getBeerById(@PathVariable UUID beerId) {
        log.debug("Retrieving beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.getBeerById(beerId));
    }

    /**
     * UpdateBeerById
     *
     * @param beerId
     * @param beerRequestDTO
     * @return BeerResponseDTO - updated beer
     */
    @PutMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> updateBeerById(
            @PathVariable UUID beerId,
            @Validated @RequestBody BeerRequestDTO beerRequestDTO) {
        log.info("Updating beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.updateBeerById(beerId, beerRequestDTO));
    }

    /**
     * PatchBeerById - partial update of beer
     *
     * @param beerId
     * @param beerRequestDTO
     * @return BeerResponseDTO - updated beer
     */
    @PatchMapping(BASE_URL_ID)
    public ResponseEntity<BeerResponseDTO> patchBeerById(
            @PathVariable UUID beerId,
            @RequestBody BeerRequestDTO beerRequestDTO) {
        log.info("Patching beer with ID: {}", beerId);
        return ResponseEntity.ok(beerService.patchBeerById(beerId, beerRequestDTO));
    }

    /**
     * DeleteBeerById
     *
     * @param beerId
     * @return ResponseEntity.noContent().build()
     */
    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteBeerById(@PathVariable UUID beerId) {
        log.info("Deleting beer with ID: {}", beerId);
        beerService.deleteBeerById(beerId);
        return ResponseEntity.noContent().build();
    }
}