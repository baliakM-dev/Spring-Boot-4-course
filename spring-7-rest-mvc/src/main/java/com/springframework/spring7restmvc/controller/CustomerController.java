package com.springframework.spring7restmvc.controller;


import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for customer resource management.
 *
 * Base path: /api/v1/customer
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class CustomerController {

    public static final String BASE_URL = "/api/v1/customer";
    public static final String BASE_URL_ID = BASE_URL + "/{customerId}";

    private final CustomerService customerService;

    @PostMapping(BASE_URL)
    public ResponseEntity<Void> createCustomer(@Validated @RequestBody CustomerRequestDTO customerRequestDTO) {
        log.info("Creating new customer: {}", customerRequestDTO.name());
        var savedCustomer = customerService.createCustomer(customerRequestDTO);
        return ResponseEntity.created(URI.create(BASE_URL + "/" + savedCustomer.id())).build();
    }

    @GetMapping(BASE_URL_ID)
    public ResponseEntity<CustomerResponseDTO> getCustomerById(@PathVariable UUID customerId) {
        log.info("Fetching customer with ID: {}", customerId);
        var customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(customer);
    }

    @GetMapping(BASE_URL)
    public ResponseEntity<List<CustomerResponseDTO>> getAllCustomers() {
        log.info("Fetching all customers");
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @PutMapping(BASE_URL_ID)
    public ResponseEntity<CustomerResponseDTO> updateCustomerById(
            @PathVariable UUID customerId,
            @Validated @RequestBody CustomerRequestDTO customerRequestDTO) {
        log.info("Updating customer with ID: {}", customerId);
        return ResponseEntity.ok(customerService.updateCustomerById(customerId, customerRequestDTO));
    }

    @DeleteMapping(BASE_URL_ID)
    public ResponseEntity<Void> deleteCustomerById(@PathVariable UUID customerId) {
        log.info("Deleting customer with ID: {}", customerId);
        customerService.deleteCustomerById(customerId);
        return ResponseEntity.noContent().build();
    }

}
