package com.springframework.spring7restmvc.services;

import com.opencsv.bean.CsvToBeanBuilder;
import com.springframework.spring7restmvc.dto.beer.BeerCSVRecord;
import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Category;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.BeerMapper;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for managing beer business logic.*
 * Handles CRUD operations with proper validation and transaction management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeerService {

    private final BeerRepository beerRepository;
    private final CategoryRepository categoryRepository;
    private final BeerMapper beerMapper;
    private final CategoryService categoryService;

    /**
     * Creates a new beer ensuring unique name across the catalog.*
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
        // 1. create beer entity from DTO
        Beer beer = beerMapper.dtoToBeer(dto);

        // 2. If dto contains ids of categories add
        // voliteľné priradenie kategórií
        if (dto.categoryIds() != null && !dto.categoryIds().isEmpty()) {

            var categories = categoryRepository.findAllById(dto.categoryIds());

            // ochrana: ak niektoré ID neexistuje, radšej failni
            if (categories.size() != dto.categoryIds().size()) {
                var foundIds = categories.stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
                var missing = dto.categoryIds().stream().filter(id -> !foundIds.contains(id)).toList();
                throw new NotFoundException("Category", "id", missing.toString());
            }

            categories.forEach(beer::addCategory); // teraz už bude fungovať
        }
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
     * Retrieves all beers from the system with optional filtering and pagination.
     *
     * @return list of all beers
     */
    @Transactional(readOnly = true)
    public Page<BeerResponseDTO> getAllBeers(String beerName,
                                             BeerStyle beerStyle,
                                             boolean showInventoryOnHand,
                                             Pageable pageable) {
        log.debug("Fetching all beers");
        Page<Beer> beerPage;

        boolean hasName = StringUtils.hasText(beerName);

        if (hasName && beerStyle == null) {
            beerPage = beerRepository.findAllByBeerNameContainingIgnoreCase(beerName, pageable);
        } else if (!hasName && beerStyle != null) {
            beerPage = beerRepository.findAllByBeerStyle(beerStyle, pageable);
        } else if (hasName) { // hasName && beerStyle != null
            beerPage = beerRepository.findAllByBeerNameContainingIgnoreCaseAndBeerStyle(beerName, beerStyle, pageable);
        } else {
            beerPage = beerRepository.findAll(pageable);
        }
        if (!showInventoryOnHand) {
            beerPage.forEach(beer -> beer.setQuantityOnHand(null));
        }

        return beerPage.map(beerMapper::beerToResponseDTO);
    }

    /**
     * Retrieve all beers from the system with optional filtering and manual pagination.
     *
     * @return list of all beers
     */
    @Transactional(readOnly = true)
    public List<BeerResponseDTO> getAllBeersManual(String beerName,
                                                   BeerStyle beerStyle,
                                                   boolean showInventoryOnHand,
                                                   Integer page,
                                                   Integer size) {
        log.debug("Fetching all beers with manual pagination");
        Page<Beer> beerPage;
        boolean hasName = StringUtils.hasText(beerName);

        if (hasName && beerStyle == null) {
            beerPage = beerRepository.findAllByBeerNameContainingIgnoreCase(beerName, Pageable.ofSize(size).withPage(page));
        } else if (!hasName && beerStyle != null) {
            beerPage = beerRepository.findAllByBeerStyle(beerStyle, Pageable.ofSize(size).withPage(page));
        } else if (hasName) { // hasName && beerStyle != null
            beerPage = beerRepository.findAllByBeerNameContainingIgnoreCaseAndBeerStyle(beerName, beerStyle, Pageable.ofSize(size).withPage(page));
        } else {
            beerPage = beerRepository.findAll(Pageable.ofSize(size).withPage(page));
        }
        if (!showInventoryOnHand) {
            beerPage.forEach(beer -> beer.setQuantityOnHand(null));
        }

        return beerPage.map(beerMapper::beerToResponseDTO).toList();
    }

    /**
     * Updates an existing beer with full replacement.*
     * Validates name uniqueness only if the name is being changed.
     * JPA Auditing automatically updates the updatedAt timestamp.
     *
     * @param beerId the beer ID to update
     * @param dto    the new beer data
     * @return updated beer
     * @throws NotFoundException               if beer not found
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
        Beer saved = beerRepository.saveAndFlush(beer);

        log.info("Updated beer: id={}, name={}", beer.getId(), beer.getBeerName());
        return beerMapper.beerToResponseDTO(saved);
    }

    /**
     * Applies a partial update to an existing beer.*
     * Only non-null fields from the patch are applied.
     * Validates name uniqueness only if the name is being changed.
     *
     * @param beerId the beer ID to update
     * @param patch  partial update data (null fields are ignored)
     * @return updated beer
     * @throws NotFoundException               if beer not found
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
     * Imports beers from CSV file.
     *
     * @param file CSV file
     */
    public List<BeerCSVRecord> importBeers(File file) {

        try {
            log.info("Importing beers from file: {}", file.getAbsolutePath());

            List<BeerCSVRecord> rec = new CsvToBeanBuilder<BeerCSVRecord>(new FileReader(file))
                    .withType(BeerCSVRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

            log.info("Import complete. {} beers imported.", rec.size());
            return rec;
        } catch (FileNotFoundException ex) {
            log.error("File not found: {}", file.getAbsolutePath());
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), ex);
        } catch (Exception e) {
            log.error("Error importing beers from file: {}", file.getAbsolutePath());
            throw new RuntimeException("Error importing beers from file: " + file.getAbsolutePath(), e);
        }
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
     * Adds a category to a beer.
     * Maintains bidirectional consistency.
     *
     * @param beerId     beer ID
     * @param categoryId category ID
     * @return updated beer with new category
     * @throws NotFoundException if beer or category not found
     */
    @Transactional
    public BeerResponseDTO addCategoryToBeer(UUID beerId, UUID categoryId) {
        log.debug("Adding category {} to beer {}", categoryId, beerId);

        Beer beer = getBeerOrThrow(beerId);
        Category category = categoryService.getCategoryOrThrow(categoryId);

        beer.addCategory(category);

        log.info("Added category {} to beer {}", categoryId, beerId);
        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Removes a category from a beer.
     * Maintains bidirectional consistency.
     *
     * @param beerId     beer ID
     * @param categoryId category ID
     * @return updated beer without the category
     * @throws NotFoundException if beer or category not found
     */
    @Transactional
    public BeerResponseDTO removeCategoryFromBeer(UUID beerId, UUID categoryId) {
        log.debug("Removing category {} from beer {}", categoryId, beerId);

        Beer beer = getBeerOrThrow(beerId);
        Category category = categoryService.getCategoryOrThrow(categoryId);

        beer.removeCategory(category);

        log.info("Removed category {} from beer {}", categoryId, beerId);
        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Sets all categories for a beer (replaces existing categories).
     * Maintains bidirectional consistency.
     *
     * @param beerId      beer ID
     * @param categoryIds set of category IDs to assign
     * @return updated beer with new categories
     * @throws NotFoundException if beer or any category not found
     */
    @Transactional
    public BeerResponseDTO setCategoriesForBeer(UUID beerId, Set<UUID> categoryIds) {
        log.debug("Setting categories for beer {}: {}", beerId, categoryIds);

        Beer beer = getBeerOrThrow(beerId);

        // Remove all existing categories
        beer.getCategories().forEach(category -> category.getBeers().remove(beer));
        beer.getCategories().clear();

        // Add new categories
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Category> categories = categoryIds.stream()
                    .map(categoryService::getCategoryOrThrow)
                    .collect(Collectors.toSet());

            categories.forEach(beer::addCategory);
        }

        log.info("Set categories for beer {}: {}", beerId, categoryIds);
        return beerMapper.beerToResponseDTO(beer);
    }

    /**
     * Validates that a beer name is unique.*
     * For updates, excludeId allows the current beer to keep its name.
     * Case-insensitive check prevents "Pilsner" and "PILSNER" duplicates.
     *
     * @param beerName  the name to validate
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