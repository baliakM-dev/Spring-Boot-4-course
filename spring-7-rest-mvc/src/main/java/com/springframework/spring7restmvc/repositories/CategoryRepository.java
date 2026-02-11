package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entity data access.
 * Includes optimized queries to prevent N+1 problems.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find category by ID with eagerly fetched beers.
     * Prevents N+1 problem when accessing category's beers.
     *
     * @param id category ID
     * @return Optional containing category with beers, or empty
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.beers WHERE c.id = :id")
    Optional<Category> findByIdWithBeers(@Param("id") UUID id);

    /**
     * Find all categories with description containing the given text (case-insensitive).
     *
     * @param description description to search for
     * @param pageable pagination parameters
     * @return page of matching categories
     */
    Page<Category> findAllByDescriptionContainingIgnoreCase(String description, Pageable pageable);

    /**
     * Check if category with given description exists (case-insensitive).
     *
     * @param description description to check
     * @return true if exists, false otherwise
     */
    boolean existsByDescriptionIgnoreCase(String description);

    /**
     * Check if category with given description exists, excluding a specific ID.
     * Useful for updates to allow keeping the same description.
     *
     * @param description description to check
     * @param id category ID to exclude
     * @return true if another category exists with this description, false otherwise
     */
    boolean existsByDescriptionIgnoreCaseAndIdNot(String description, UUID id);
}