package com.springframework.spring7restmvc.integrationTests;

import com.springframework.spring7restmvc.controllers.CustomerController;
import com.springframework.spring7restmvc.entities.Customer;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CustomerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Customer API endpoints.
 * Tests the full stack: Controller -> Service -> Repository -> Database
 * Uses TestContainers for an isolated MySQL database per test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class CustomerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ==================== Test Fixtures ====================
    private Customer createCustomer(String name) {
        return Customer.builder()
                .name(name)
                .build();
    }

    private String createCustomerJson(String name) {
        return """
                {
                "name": "%s"
                }
                """.formatted(name);
    }

    private String escapeJson(String json) {
        return json.replace("\"", "\\\"");
    }

    // ==================== Create Customer Tests ====================

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {

        @Test
        void should_CreateAndRetrieveCustomer() throws Exception {
            // Given
            String customerJson = createCustomerJson("Martin");

            // When - Create
            MvcResult createResult = mockMvc.perform(post(CustomerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customerJson))
                    .andExpect(status().isCreated())
                    .andReturn();

            String location = createResult.getResponse().getHeader("Location");
            assertThat(location).isNotNull();

            // Then - Retrieve
            mockMvc.perform(get(location))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Martin"));
        }

        @Test
        void should_Return400ForInvalidJson() throws Exception {
            // Given - missing required field
            String invalidJson = """
                    {
                      "name": ""
                    }
                    """;

            // When/Then
            mockMvc.perform(post(CustomerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"));
        }

        @Test
        void should_HandleMalformedJson() throws Exception {
            // Given - invalid JSON
            String malformedJson = "{ name: 'invalid' }";

            // When/Then
            mockMvc.perform(post(CustomerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid JSON input"));
        }

        @Test
        void should_PreventDuplicateCustomerName() throws Exception {
            // Given - existing customer
            customerRepository.save(createCustomer("Test Customer"));

            String duplicateJson = createCustomerJson("Test Customer");

            // When/Then
            mockMvc.perform(post(CustomerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource already exists"));
        }
    }

    @Nested
    @DisplayName("Customer Update Tests")
    class CustomerUpdateTests {

        @Test
        void sould_UpdateCustomer() throws Exception {
            // GIvne - existing customer
            Customer customer = customerRepository.save(createCustomer("Test Customer"));

            // When - update
            String updatedJson = createCustomerJson("Test Customer 2");

            // Then - should succeed
            mockMvc.perform(put(CustomerController.BASE_URL + "/" + customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatedJson))
                    .andExpect(status().isOk());

            // Verify in database
            Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
            assertThat(updatedCustomer.getName()).isEqualTo("Test Customer 2");
            assertThat(updatedCustomer.getUpdatedAt()).isNotNull();
        }

        @Test
        void sould_PreventUpdateWithDuplicateName() throws Exception {
            // Given - existing customer
            Customer customer1 = customerRepository.save(createCustomer("Test Customer 1"));
            customerRepository.save(createCustomer("Test Customer 2"));

            // When - update with duplicate name
            String duplicateJson = createCustomerJson("Test Customer 2");

            // Then - should fail
            mockMvc.perform(put(CustomerController.BASE_URL + "/" + customer1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource already exists"));
        }

        @Test
        void should_pdateTimestampsOnUpdate() throws Exception {
            // Given - existing customer
            Customer customer = customerRepository.saveAndFlush(createCustomer("Test Customer"));
            Customer persisted = customerRepository.findById(customer.getId()).orElseThrow();

            LocalDateTime originalCreated = persisted.getCreatedAt();
            LocalDateTime originalUpdated = persisted.getUpdatedAt();

            String updatedJson = createCustomerJson("Test Customer 2");

            // When - update
            mockMvc.perform(put(CustomerController.BASE_URL + "/" + customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatedJson))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            // Then
            Customer updated = customerRepository.findById(customer.getId()).orElseThrow();

            assertThat(updated.getCreatedAt()).isEqualTo(originalCreated);
            assertThat(updated.getUpdatedAt()).isNotEqualTo(originalUpdated);

            // isAfter
            assertThat(updated.getUpdatedAt()).isAfter(originalUpdated);
        }

        @Test
        void should_Return404ForNonExistingCustomer() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();

            String updateJson = createCustomerJson("Martin");

            // When/Then
            mockMvc.perform(put(CustomerController.BASE_URL + "/" + randomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource not found"));
        }
    }

    // ==================== Delete Customer Tests ====================

    @Nested
    @DisplayName("Delete Customer Tests")
    class DeleteCustomerTests {

        @Test
        void sould_DeleteCustomer() throws Exception {
            // Given - existing customer
            Customer customer = customerRepository.save(createCustomer("Test Customer"));

            // When - delete
            mockMvc.perform(delete(CustomerController.BASE_URL + "/" + customer.getId()))
                    .andExpect(status().isNoContent());

            // Then - verify deletion
            assertThat(customerRepository.findById(customer.getId())).isEmpty();
        }
    }

    @Test
    void should_Return404ForNonExistingCustomer() throws Exception {
        // GIven
        UUID randomId = UUID.randomUUID();

        // When/Then
        mockMvc.perform(delete(CustomerController.BASE_URL + "/" + randomId))
                .andExpect(status().isNotFound());
    }

    // ==================== Qery Customer Tests ====================

    @Nested
    @DisplayName("Query Customer Tests")
    class QueryCustomerTests {

        @Test
        void should_GetAllCustomers() throws Exception {
            // Given - multiple customers
            customerRepository.save(createCustomer("Test Customer 1"));
            customerRepository.save(createCustomer("Test Customer 2"));
            customerRepository.save(createCustomer("Test Customer 3"));
            customerRepository.save(createCustomer("Test Customer 4"));
            customerRepository.save(createCustomer("Test Customer 5"));
            customerRepository.save(createCustomer("Test Customer 6"));
            customerRepository.save(createCustomer("Test Customer 7"));
            customerRepository.save(createCustomer("Test Customer 8"));
            customerRepository.save(createCustomer("Test Customer 9"));
            customerRepository.save(createCustomer("Test Customer 10"));
            customerRepository.save(createCustomer("Test Customer 11"));

            // When/Then
            mockMvc.perform(get(CustomerController.BASE_URL)
                            .param("size", "5")
                            .param("sort", "name,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.content[0].name").value("Test Customer 1"))
                    .andExpect(jsonPath("$.content[1].name").value("Test Customer 10"))
                    .andExpect(jsonPath("$.content[2].name").value("Test Customer 11"))
                    .andExpect(jsonPath("$.content[3].name").value("Test Customer 2"))
                    .andExpect(jsonPath("$.content[4].name").value("Test Customer 3"));
        }

        @Test
        void should_FilteredCustomersByName() throws Exception {
            //Give
            customerRepository.save(createCustomer("Test Customer 1"));
            customerRepository.save(createCustomer("Test Customer 2"));
            customerRepository.save(createCustomer("Test Customer 3"));

            // When/Then
            mockMvc.perform(get(CustomerController.BASE_URL)
                            .param("name", "Test Customer 2")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Test Customer 2"));
        }
    }
}
