package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for Customer entity data access.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Checks if a customer with the given name exists (case-insensitive).
     *
     * @param name the customer name to check
     * @return true if a customer exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if a customer with the given name exists (case-insensitive) and excludes the given ID.
     * Useful for update operations where we want to allow keeping the same name.
     *
     *
     * @param name the customer name to check
     * @param id the ID to exclude from the check
     * @return true if a customer exists, false otherwise
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /**
     * Find all customers by name (case-insensitive).
     *
     * @param name the customer name to search for
     * @param pageable pagination parameters
     * @return a page of customers matching the name
     */
    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
