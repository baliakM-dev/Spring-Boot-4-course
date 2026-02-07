package com.springframework.spring7restmvc.services;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.BeerMapper;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for managing beer business logic.
 *
 * <p>This service handles all business operations related to beers, including:
 * <ul>
 *   <li>Beer creation with duplicate name validation</li>
 *   <li>DTO to Entity mapping and vice versa</li>
 *   <li>Timestamp management</li>
 * </ul>
 * </p>
 *
 * @author Martin Baliak
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor  // Lepšie než @AllArgsConstructor pre immutable services
@Slf4j
public class BeerService {

    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    /**
     * Creates a new beer in the system.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Validates that beer name doesn't already exist (case-insensitive)</li>
     *   <li>Maps DTO to Entity</li>
     *   <li>Sets creation and update timestamps</li>
     *   <li>Persists the entity to database with immediate flush</li>
     *   <li>Maps saved entity back to response DTO</li>
     * </ol>
     * </p>
     *
     * <p><strong>Business rules:</strong>
     * <ul>
     *   <li>Beer name must be unique (case-insensitive check)</li>
     *   <li>All required fields must be present and valid (enforced by DTO validation)</li>
     * </ul>
     * </p>
     *
     * @param dto the beer creation request containing all required beer information
     * @return {@link BeerResponseDTO} containing the created beer with generated ID and timestamps
     * @throws ResourceAlreadyExistsExceptions                         if a beer with the same name already exists
     * @throws org.springframework.dao.DataIntegrityViolationException if database constraints are violated
     */
    @Transactional  // Zabezpečí rollback pri chybe
    public BeerResponseDTO createNewBeer(BeerRequestDTO dto) {
        log.debug("Creating new beer with name: {}", dto.beerName());

        // 1) Business rule – beer name must be unique (case-insensitive)
        validateBeerNameUniqueness(dto.beerName());

        // 2) Map DTO -> Entity
        Beer beer = beerMapper.dtoToBeer(dto);

        // Note: Timestamps could be managed by JPA @PrePersist/@PreUpdate,
        // but we set them explicitly here for clarity and control
        beer.setCreatedAt(LocalDateTime.now());
        beer.setUpdatedAt(LocalDateTime.now());

        // 3) Save with an immediate flush to catch DB-level constraint violations
        // This helps detect race conditions where another transaction might have
        // created a beer with the same name between our check and save
        Beer saved = beerRepository.saveAndFlush(beer);

        log.info("Successfully created beer with ID: {} and name: {}",
                saved.getId(), saved.getBeerName());

        // 4) Map Entity -> Response DTO
        return beerMapper.beerToResponseDTO(saved);
    }

    /**
     * Finds a beer by its unique identifier.
     *
     * @param id the beer ID to search for
     * @return {@link BeerResponseDTO} containing the beer data
     * @throws NotFoundException if no beer exists with the given ID
     */
    @Transactional(readOnly = true)
    public BeerResponseDTO getBeerById(UUID id) {
        log.debug("Fetching beer with ID: {}", id);

        Beer beer = getBeerOrThrow(id);
        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Retrieves all beers from the system.
     *
     * @return list of all beers, empty list if no beers exist
     */
    @Transactional(readOnly = true)
    public List<BeerResponseDTO> getAllBeers() {
        log.debug("Fetching all beers");

        List<Beer> beers = beerRepository.findAll();

        return beers.stream()
                .map(beerMapper::beerToResponseDTO)
                .toList();
    }

    /**
     * Updates an existing beer resource.
     *
     * @param beerId the unique identifier of the beer to update
     * @param dto    the updated beer data, validated against {@link BeerRequestDTO}
     * @return HTTP 200 OK with updated beer data {@link BeerResponseDTO}
     * @throws com.springframework.spring7restmvc.exceptions.NotFoundException               if beer not found
     * @throws com.springframework.spring7restmvc.exceptions.MethodArgumentNotValidException if validation fails
     * @throws com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions if name exists
     */
    @Transactional
    public BeerResponseDTO updateBeerById(UUID beerId, BeerRequestDTO dto) {
        Beer beer = getBeerOrThrow(beerId); // managed entita s vyplnenou version

        validateBeerNameUniqueness(dto.beerName());

        beerMapper.updateBeerFromDto(dto, beer);
        return beerMapper.beerToResponseDTO(beer); // netreba save()
    }

    /**
     * Applies a partial update to an existing beer.
     *
     * <p>Only non-null fields from the patch object are applied.</p>
     *
     * @param beerId the unique identifier of the beer to update
     * @param patch  partial update data
     * @return updated beer
     * @throws NotFoundException               if beer not found
     * @throws ResourceAlreadyExistsExceptions if beer name already exists
     */
    @Transactional
    public BeerResponseDTO patchBeerById(UUID beerId, BeerRequestDTO patch) {
        Beer beer = getBeerOrThrow(beerId);
        validateBeerNameUniqueness(patch.beerName());

        if (patch.beerName() != null) beer.setBeerName(patch.beerName());
        if (patch.beerStyle() != null) beer.setBeerStyle(patch.beerStyle());
        if (patch.upc() != null) beer.setUpc(patch.upc());
        if (patch.quantityOnHand() != null) beer.setQuantityOnHand(patch.quantityOnHand());
        if (patch.price() != null) beer.setPrice(patch.price());

        beer.setUpdatedAt(LocalDateTime.now());

        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Deletes a specific beer by its unique identifier.
     * @param beerId the unique identifier of the beer to delete
     */
    @Transactional
    public void deleteBeerById(UUID beerId) {
        var beer = getBeerOrThrow(beerId);
        beerRepository.delete(beer);
        log.info("Successfully deleted beer with ID: {}", beerId);
    }


    /**
     * Validates that a beer with the given name doesn't already exist.
     *
     * <p>Performs case-insensitive check to prevent duplicate beer names
     * that differ only in capitalization (e.g., "Pilsner" vs "PILSNER").</p>
     *
     * @param beerName the beer name to validate
     * @throws ResourceAlreadyExistsExceptions if a beer with this name already exists
     */
    private void validateBeerNameUniqueness(String beerName) {
        if (beerRepository.existsByBeerNameIgnoreCase(beerName)) {
            log.warn("Attempt to create beer with duplicate name: {}", beerName);
            throw new ResourceAlreadyExistsExceptions(
                    "Beer",
                    "beerName",
                    beerName
            );
        }
    }

    /**
     * Retrieves a beer by its unique identifier or throws an exception if not found.
     *
     * @param beerId the unique identifier of the beer to retrieve
     * @return the beer entity with the given ID {@link BeerResponseDTO}
     * @throws com.springframework.spring7restmvc.exceptions.NotFoundException if beer not found
     */
    private Beer getBeerOrThrow(UUID beerId) {
        return beerRepository.findById(beerId)
                .orElseThrow(() -> {
                    log.warn("Beer not found with ID: {}", beerId);
                    return new NotFoundException("Beer", "id", beerId.toString());
                });
    }
}