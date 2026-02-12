package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Customer;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void should_SetCreatedAndUpdatedDates_OnPersist() {
        Customer customer = Customer.builder()
                .name("Martin")
                .build();

        Customer saved = customerRepository.saveAndFlush(customer);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void should_UpdateUpdatedAt_OnUpdate() {
        Customer customer = customerRepository.saveAndFlush(
                Customer.builder().name("Martin").build()
        );

        LocalDateTime originalUpdated = customer.getUpdatedAt();

        customer.setName("Martin Updated");
        Customer updated = customerRepository.saveAndFlush(customer);

        assertThat(updated.getUpdatedAt()).isAfter(originalUpdated);
    }

    @Test
    void should_EnforceUniqueConstraintOnName() {
        customerRepository.saveAndFlush(
                Customer.builder().name("Martin").build()
        );

        Customer duplicate = Customer.builder()
                .name("Martin")
                .build();

        assertThatThrownBy(() ->
                customerRepository.saveAndFlush(duplicate)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_IncrementVersion_OnUpdate() {
        Customer customer = customerRepository.saveAndFlush(
                Customer.builder().name("Martin").build()
        );

        Integer originalVersion = customer.getVersion();

        customer.setName("New Name");
        Customer updated = customerRepository.saveAndFlush(customer);

        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
    }

}