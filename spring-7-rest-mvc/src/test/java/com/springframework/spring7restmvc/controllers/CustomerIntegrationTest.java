package com.springframework.spring7restmvc.controllers;

import com.springframework.spring7restmvc.entities.Customer;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("localmysql")
public class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private BeerRepository beerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveCustomer() throws Exception {
        // Given
        String customerJson = """
                {
                    "name": "Test Customer"
                }
                """;

        // When - create
        MvcResult createResult = mockMvc.perform(post(CustomerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Then - retrieve
        String location = createResult.getResponse().getHeader("Location");
        assert location != null;
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Customer"));
    }

    @Test
    void shouldAllowUpdateWithSameName() throws Exception {
        // Given - existing
        Customer customer = Customer.builder().name("Test Customer").build();

        Customer saved = customerRepository.save(customer);

        // When
        String customerJson = """
                {
                    "name": "Test Customer"
                }
                """;

        // Then - should update
        mockMvc.perform(put(CustomerController.BASE_URL_ID, saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isOk());

        // Then - should retrieve updated
        mockMvc.perform(get(CustomerController.BASE_URL_ID, saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Customer"));
    }

    @Test
    void shouldRejectUpdateWhenNameBelongsToAnotherCustomer() throws Exception {
        Customer a = customerRepository.save(
                Customer.builder().name("Alice").build()
        );

        Customer b = customerRepository.save(
                Customer.builder().name("Bob").build()
        );

        String json = """
                { "name": "Alice" }
                """;

        mockMvc.perform(put(CustomerController.BASE_URL_ID, b.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteCustomer() throws Exception {
        // Given
        Customer customer = customerRepository.save(Customer.builder().name("Test Customer").build());

        // When
        mockMvc.perform(delete(CustomerController.BASE_URL_ID, customer.getId()))
                .andExpect(status().isNoContent());

        // Then - verify deletion
        assertThat(beerRepository.findById(customer.getId()))
                .isEmpty();
    }

    @Test
    void shouldReturn404ForNonExistingCustomer() throws Exception {
        // Given
        String randomId = UUID.randomUUID().toString();

        mockMvc.perform(get(CustomerController.BASE_URL_ID, randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void shouldGetAllCustomers() throws Exception {
        // Given
        customerRepository.save(Customer.builder().name("Alice").build());
        customerRepository.save(Customer.builder().name("Bob").build());

        // When / Then
        mockMvc.perform(get(CustomerController.BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void shouldHandleInvalidJson() throws Exception{
        // Given - invalid JSON
        String malformedJson = "{ name: 'invalid' }";

        // When/Then
        mockMvc.perform(post(CustomerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCustomerById() throws Exception{
        // Given
        Customer customer = customerRepository.save(Customer.builder().name("Alice").build());

        // When/Then
        mockMvc.perform(get(CustomerController.BASE_URL_ID, customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }
}
