package com.springframework.spring7restmvc.dto.customer;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for customer creation requests.
 *
 * <p>This DTO contains all required information for creating a new customer
 * and includes validation constraints to ensure data integrity.</p>
 *
 * @param name the customer's name must not be blank
 */
public record CustomerRequestDTO(

        @NotBlank(message = "{customer.name.notblank}")
        String name
) {
}
