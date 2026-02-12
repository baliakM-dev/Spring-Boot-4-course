package com.springframework.spring7restmvc.entities;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity tests for {@link Customer}.
 */
class CustomerTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_HaveViolation_WhenNameBlank() {
        Customer customer = Customer.builder()
                .name("")
                .build();

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void should_HaveViolation_WhenNameTooLong() {
        String longName = "A".repeat(101);

        Customer customer = Customer.builder()
                .name(longName)
                .build();

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void should_BeValid_WhenNameCorrect() {
        Customer customer = Customer.builder()
                .name("Martin")
                .build();

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        assertThat(violations).isEmpty();
    }

}