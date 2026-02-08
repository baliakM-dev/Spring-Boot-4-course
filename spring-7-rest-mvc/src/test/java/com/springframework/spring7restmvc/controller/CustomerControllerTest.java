package com.springframework.spring7restmvc.controller;

import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.mapper.CustomerMapper;
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

    }

    @Test
    void testGetCustomerByIdNotFound() throws Exception {

    }

    @Test
    void testGetCustomerByIdSuccess() throws Exception {

    }

    @Test
    void testGetAllCustomersSuccess() throws Exception {

    }

    @Test
    void testDeleteCustomerByIdSuccess() throws Exception {

    }

    @Test
    void testUpdateCustomerByIdSuccess() throws Exception {

    }

    @Test
    void testUpdateCustomerByIdNotFound() throws Exception {

    }

}
