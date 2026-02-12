package com.springframework.spring7restmvc.controllers;


import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for customer resource management.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class CustomerController {

    public static final String BASE_URL = "/api/v1/customers";
    public static final String BASE_URL_ID = BASE_URL + "/{customerId}";

    private final CustomerService customerService;

    /**
     * Create a new customer.
     *
     * @param customerRequestDTO customer creation request
     * @return status code 201 Created and location of created resource
     */
    @PostMapping(BASE_URL)
    public ResponseEntity<Void> createCustomer(@Validated @RequestBody CustomerRequestDTO customerRequestDTO) {
        log.info("Creating new customer: {}", customerRequestDTO.name());
        var savedCustomer = customerService.createCustomer(customerRequestDTO);
        return ResponseEntity.created(URI.create(BASE_URL + "/" + savedCustomer.id())).build();
    }

    /**
     * Get all customers with pagination.
     *
     * @return list of customers
     */
    @GetMapping(BASE_URL)
    public ResponseEntity<Page<CustomerResponseDTO>> getAllCustomers(
            @RequestParam(required = false) String name,
            Pageable pageable) {
        log.info("GET /api/v1/customers , pageable: {}", pageable);
        return ResponseEntity.ok(customerService.getAllCustomers(name, pageable));
    }

    /**
     * Get a specific customer by ID.
     *
     * @param customerId customer UUID
     * @return customer data
     */
    @GetMapping(BASE_URL_ID)
    public ResponseEntity<CustomerResponseDTO> getCustomerById(@PathVariable UUID customerId) {
        log.info("Fetching customer with ID: {}", customerId);
        var customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Update an existing customer.
     *
     * @param customerId customer UUID
     * @param customerRequestDTO customer data
     * @return updated customer data
     */
    @PutMapping(BASE_URL_ID)
    public ResponseEntity<CustomerResponseDTO> updateCustomerById(
            @PathVariable UUID customerId,
            @Validated @RequestBody CustomerRequestDTO customerRequestDTO) {
        log.info("Updating customer with ID: {}", customerId);
        return ResponseEntity.ok(customerService.updateCustomerById(customerId, customerRequestDTO));
    }

    /**
     * Delete a customer by ID.
     *
     * @param customerId customer UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteCustomerById(@PathVariable UUID customerId) {
        log.info("Deleting customer with ID: {}", customerId);
        customerService.deleteCustomerById(customerId);
        return ResponseEntity.noContent().build();
    }

}
