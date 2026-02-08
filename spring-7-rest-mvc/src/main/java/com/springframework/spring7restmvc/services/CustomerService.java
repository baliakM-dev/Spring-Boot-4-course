package com.springframework.spring7restmvc.services;


import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.entities.Customer;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.CustomerMapper;
import com.springframework.spring7restmvc.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for managing customer business logic.
 *
 * Handles CRUD operations with proper validation and transaction management.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    /**
     * Creates a new customer ensuring unique name across the catalog.
     *
     * Uses save() which delegates to EntityManager and triggers JPA Auditing for timestamp management.
     *
     * @param dto the customer creation request
     * @return created customer with generated ID and timestamps
     * @throws ResourceAlreadyExistsExceptions if customer name already exists
     */
    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO dto) {
        log.debug("Creating customer with name: {}", dto.name());

        validateCustomerNameUniqueness(dto.name(), null);

        Customer customer = customerMapper.dtoToCustomer(dto);
        customer = customerRepository.save(customer);

        log.info("Customer created with ID: {}", customer.getId());
        return customerMapper.customerToResponseDto(customer);

    }

    /**
     * Retrieves a customer by its unique identifier.
     *
     * @param id the customer ID
     * @return customer data
     * @throws NotFoundException if customer not found
     */
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerById(UUID id) {
        log.debug("Fetching customer with ID: {}", id);
        return customerMapper.customerToResponseDto(getCustomerOrThrow(id));
    }

    /**
     * Retrieves all customers from the catalog.
     *
     * @return list of customers
     */
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getAllCustomers() {
        log.debug("Fetching all customers");
        return customerRepository.findAll()
                .stream()
                .map(customerMapper::customerToResponseDto)
                .toList();
    }

    /**
     *  Updates an existing customer with full replacement.
     *
     *  Validates name uniqueness only if the name is being changed.
     *  JPA Auditing is used for timestamp management.
     *
     * @param id the customer ID
     * @param dto the customer update request
     * @return updated customer
     * @throws NotFoundException if customer not found
     * @throws ResourceAlreadyExistsExceptions if the new name conflicts with another customer
     */
    @Transactional
    public CustomerResponseDTO updateCustomerById(UUID id, CustomerRequestDTO dto) {
        log.debug("Updating customer with ID: {}", id);

        Customer customer = getCustomerOrThrow(id);

        if (!customer.getName().equalsIgnoreCase(dto.name())) {
            validateCustomerNameUniqueness(dto.name(), id);
        }

        customerMapper.updateCustomerFromDto(dto, customer);

        log.info("Customer updated with ID: {}", id);
        return customerMapper.customerToResponseDto(customerRepository.save(customer));
    }


    /**
     * Validates that a customer name is unique.
     *
     * For updates, excludeId allows the current beer to keep its name.
     * Case-insensitive check prevents "Pilsner" and "PILSNER" duplicates.
     *
     * @param name the name to validate
     * @param excludeId the beer ID to exclude from check (null for create operations)
     * @throws ResourceAlreadyExistsExceptions if name exists
     */
    private void validateCustomerNameUniqueness(String name, UUID excludeId) {
        boolean exists = excludeId == null
                ? customerRepository.existsByNameIgnoreCase(name)
                : customerRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);

        if (exists) {
            log.warn("Customer name already exists: {}", name);
            throw new ResourceAlreadyExistsExceptions("Customer", "name", name);
        }
    }

    /**
     * Retrieves a customer or throws NotFoundException.
     *
     * @param id the customer ID
     * @return the customer entity
     * @throws NotFoundException if customer not found
     */
    private Customer getCustomerOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer not found with ID: {}", id);
                    return new NotFoundException("Customer", "id", id.toString());
                });
    }
}
