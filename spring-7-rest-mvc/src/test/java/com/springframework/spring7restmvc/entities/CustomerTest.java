package com.springframework.spring7restmvc.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validCustomerHasNoViolations() {
        Customer customer = Customer.builder()
                .name("")
                .build();
        var violations = validator.validate(customer);
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void validCustomerHasNoViolations2() {
        Customer customer = Customer.builder()
                .name("Test")
                .build();
        var violations = validator.validate(customer);
        assertThat(violations).isEmpty();
    }

    @Test
    void createCustomer() {
        Customer customer = Customer.builder()
                .name("Test")
                .build();
        assertThat(customer).isNotNull();
    }
}