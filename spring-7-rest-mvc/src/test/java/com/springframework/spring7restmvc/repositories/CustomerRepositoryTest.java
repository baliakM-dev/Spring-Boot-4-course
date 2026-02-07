package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Customer;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void createCustomerJpaTest() {
        Customer customer = Customer.builder()
                .name("Test")
                .build();

        customerRepository.save(customer);
        assertThat(customer.getId()).isNotNull();
        assertThat(customerRepository.findAll()).hasSize(1);
        System.out.println(customer.getId());
    }

    @Test
    void saveShouldFailWhenNameNull() {
        Customer customer = Customer.builder()
                .name(null) // poruší @NotBlank
                .build();

        assertThrows(ConstraintViolationException.class, () ->
                customerRepository.saveAndFlush(customer)
        );
    }

}