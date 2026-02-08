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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for managing beer business logic.
 *
 * Handles CRUD operations with proper validation and transaction management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeerService {

    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    /**
     * Creates a new beer ensuring unique name across the catalog.
     *
     * Uses save() which delegates to EntityManager and triggers JPA Auditing
     * for automatic timestamp management.
     *
     * @param dto the beer creation request
     * @return created beer with generated ID and timestamps
     * @throws ResourceAlreadyExistsExceptions if beer name exists (case-insensitive)
     */
    @Transactional
    public BeerResponseDTO createNewBeer(BeerRequestDTO dto) {
        log.debug("Creating new beer with name: {}", dto.beerName());

        validateBeerNameUniqueness(dto.beerName(), null);

        Beer beer = beerMapper.dtoToBeer(dto);
        Beer saved = beerRepository.save(beer);

        log.info("Created beer: id={}, name={}", saved.getId(), saved.getBeerName());
        return beerMapper.beerToResponseDTO(saved);
    }

    /**
     * Retrieves a beer by its unique identifier.
     *
     * @param id the beer ID
     * @return beer data
     * @throws NotFoundException if beer not found
     */
    @Transactional(readOnly = true)
    public BeerResponseDTO getBeerById(UUID id) {
        log.debug("Fetching beer with ID: {}", id);
        return beerMapper.beerToResponseDTO(getBeerOrThrow(id));
    }

    /**
     * Retrieves all beers from the system.
     *
     * @return list of all beers
     */
    @Transactional(readOnly = true)
    public List<BeerResponseDTO> getAllBeers() {
        log.debug("Fetching all beers");
        return beerRepository.findAll()
                .stream()
                .map(beerMapper::beerToResponseDTO)
                .toList();
    }

    /**
     * Updates an existing beer with full replacement.
     *
     * Validates name uniqueness only if the name is being changed.
     * JPA Auditing automatically updates the updatedAt timestamp.
     *
     * @param beerId the beer ID to update
     * @param dto the new beer data
     * @return updated beer
     * @throws NotFoundException if beer not found
     * @throws ResourceAlreadyExistsExceptions if the new name conflicts with existing beer
     */
    @Transactional
    public BeerResponseDTO updateBeerById(UUID beerId, BeerRequestDTO dto) {
        log.debug("Updating beer with ID: {}", beerId);

        Beer beer = getBeerOrThrow(beerId);

        // Only validate name uniqueness if the name is changing
        if (!beer.getBeerName().equalsIgnoreCase(dto.beerName())) {
            validateBeerNameUniqueness(dto.beerName(), beerId);
        }

        beerMapper.updateBeerFromDto(dto, beer);

        log.info("Updated beer: id={}, name={}", beer.getId(), beer.getBeerName());
        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Applies a partial update to an existing beer.
     *
     * Only non-null fields from the patch are applied.
     * Validates name uniqueness only if name is being changed.
     *
     * @param beerId the beer ID to update
     * @param patch partial update data (null fields are ignored)
     * @return updated beer
     * @throws NotFoundException if beer not found
     * @throws ResourceAlreadyExistsExceptions if new name conflicts
     */
    @Transactional
    public BeerResponseDTO patchBeerById(UUID beerId, BeerRequestDTO patch) {
        log.debug("Patching beer with ID: {}", beerId);

        Beer beer = getBeerOrThrow(beerId);

        // Only validate if name is being changed and is different
        if (patch.beerName() != null && !beer.getBeerName().equalsIgnoreCase(patch.beerName())) {
            validateBeerNameUniqueness(patch.beerName(), beerId);
        }

        applyPatch(patch, beer);

        log.info("Patched beer: id={}, name={}", beer.getId(), beer.getBeerName());
        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Deletes a beer by its ID.
     *
     * @param beerId the beer ID to delete
     * @throws NotFoundException if beer not found
     */
    @Transactional
    public void deleteBeerById(UUID beerId) {
        log.debug("Deleting beer with ID: {}", beerId);

        Beer beer = getBeerOrThrow(beerId);
        beerRepository.delete(beer);

        log.info("Deleted beer: id={}", beerId);
    }

    /**
     * Applies non-null fields from patch to beer entity.
     */
    private void applyPatch(BeerRequestDTO patch, Beer beer) {
        if (patch.beerName() != null) {
            beer.setBeerName(patch.beerName());
        }
        if (patch.beerStyle() != null) {
            beer.setBeerStyle(patch.beerStyle());
        }
        if (patch.upc() != null) {
            beer.setUpc(patch.upc());
        }
        if (patch.quantityOnHand() != null) {
            beer.setQuantityOnHand(patch.quantityOnHand());
        }
        if (patch.price() != null) {
            beer.setPrice(patch.price());
        }
    }

    /**
     * Validates that a beer name is unique.
     *
     * For updates, excludeId allows the current beer to keep its name.
     * Case-insensitive check prevents "Pilsner" and "PILSNER" duplicates.
     *
     * @param beerName the name to validate
     * @param excludeId the beer ID to exclude from check (null for create operations)
     * @throws ResourceAlreadyExistsExceptions if name exists
     */
    private void validateBeerNameUniqueness(String beerName, UUID excludeId) {
        boolean exists = excludeId == null
                ? beerRepository.existsByBeerNameIgnoreCase(beerName)
                : beerRepository.existsByBeerNameIgnoreCaseAndIdNot(beerName, excludeId);

        if (exists) {
            log.warn("Beer name already exists: {}", beerName);
            throw new ResourceAlreadyExistsExceptions("Beer", "beerName", beerName);
        }
    }

    /**
     * Retrieves a beer or throws NotFoundException.
     *
     * @param beerId the beer ID
     * @return the beer entity
     * @throws NotFoundException if not found
     */
    private Beer getBeerOrThrow(UUID beerId) {
        return beerRepository.findById(beerId)
                .orElseThrow(() -> {
                    log.warn("Beer not found with ID: {}", beerId);
                    return new NotFoundException("Beer", "id", beerId.toString());
                });
    }
}