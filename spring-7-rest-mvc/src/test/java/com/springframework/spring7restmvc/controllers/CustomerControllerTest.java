package com.springframework.spring7restmvc.controllers;

import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.services.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test for CustomerController
 * <p>
 * Uses MockMvc to test endpoints with mocked service layer.
 */
@WebMvcTest(CustomerController.class)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateCustomerTest() throws Exception {
        // Given
        CustomerRequestDTO request = new CustomerRequestDTO("Test");

        UUID customerId = UUID.randomUUID();

        CustomerResponseDTO response = new CustomerResponseDTO(
                customerId,
                request.name(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(customerService.createCustomer(any(CustomerRequestDTO.class)))
                .willReturn(response);

        // When/Then
        mockMvc.perform(post(CustomerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString(customerId.toString())));

        then(customerService).should().createCustomer(any(CustomerRequestDTO.class));
    }

    @Test
    void testCreateCustomerWithValidationFalis() throws Exception {
        // Given - name validation errros
        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", "");

        // When/Then
        mockMvc.perform(post(CustomerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void testCreateCustomerWithNameAlreadyExists() throws Exception {
        // Given
        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", "Martin");

        given(customerService.createCustomer(any(CustomerRequestDTO.class)))
                .willThrow(new ResourceAlreadyExistsExceptions("Customer", "name", "Martin"));

        // When/Then
        mockMvc.perform(post(CustomerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerMap)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));
    }

    @Test
    void testGetCustomerByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        given(customerService.getCustomerById(nonExistentId))
                .willThrow(new NotFoundException("Customer", "id", nonExistentId.toString()));

        // When/Then
        mockMvc.perform(get(CustomerController.BASE_URL_ID, nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value(containsString("Customer not found")))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testGetCustomerByIdSuccess() throws Exception {
        UUID customerId = UUID.randomUUID();

        CustomerResponseDTO mockResponse = new CustomerResponseDTO(
                customerId,
                "Test",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(customerService.getCustomerById(customerId)).willReturn(mockResponse);

        // When/Then
        mockMvc.perform(get(CustomerController.BASE_URL_ID, customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    void testGetAllCustomersSuccess() throws Exception {
        List<CustomerResponseDTO> customer = List.of(
                new CustomerResponseDTO(UUID.randomUUID(), "Test 1", LocalDateTime.now(), LocalDateTime.now()),
                new CustomerResponseDTO(UUID.randomUUID(), "Test 2", LocalDateTime.now(), LocalDateTime.now())
        );

        given(customerService.getAllCustomers()).willReturn(customer);

        // When/Then
        mockMvc.perform(get(CustomerController.BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test 1"))
                .andExpect(jsonPath("$[1].name").value("Test 2"));
    }

    @Test
    void testDeleteCustomerByIdSuccess() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();

        willDoNothing().given(customerService).deleteCustomerById(beerId);

        // When/Then
        mockMvc.perform(delete(CustomerController.BASE_URL_ID, beerId))
                .andExpect(status().isNoContent());

        then(customerService).should().deleteCustomerById(beerId);
    }

    @Test
    void testDeleteCustomerByIdNotFound() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();

        willThrow(new NotFoundException("Customer", "id", beerId.toString()))
                .given(customerService).deleteCustomerById(beerId);

        // When/Then
        mockMvc.perform(delete(CustomerController.BASE_URL_ID, beerId))
                .andExpect(status().isNotFound());

        then(customerService).should().deleteCustomerById(beerId);
    }


    @Test
    void testUpdateCustomerByIdSuccess() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();

        CustomerRequestDTO request = new CustomerRequestDTO("Updated Name");

        CustomerResponseDTO response = new CustomerResponseDTO(
                beerId,
                request.name(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(customerService.updateCustomerById(beerId, request)).willReturn(response);

        // When/Then
        mockMvc.perform(put(CustomerController.BASE_URL_ID, beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        then(customerService).should().updateCustomerById(eq(beerId), any(CustomerRequestDTO.class));
    }

    @Test
    void testUpdateCustomerByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        String body = """
                {
                "name": "Updated Name"
                }
                """;

        given(customerService.updateCustomerById(eq(nonExistentId), any(CustomerRequestDTO.class)))
                .willThrow(new NotFoundException("Customer", "id", nonExistentId.toString()));

        // When/Then
        mockMvc.perform(put(CustomerController.BASE_URL_ID, nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCustomerByIdInvalidRequest() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        CustomerRequestDTO request = new CustomerRequestDTO("");

        // When/Then
        mockMvc.perform(put(CustomerController.BASE_URL_ID, nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }
}
