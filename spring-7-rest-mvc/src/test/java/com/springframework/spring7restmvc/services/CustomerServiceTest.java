package com.springframework.spring7restmvc.services;

import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.entities.Customer;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.CustomerMapper;
import com.springframework.spring7restmvc.repositories.CustomerRepository;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerService}.
 * <p>
 * Focus:
 * - uniqueness validation
 * - not found / conflict exceptions
 * <p>
 * Note: These are pure unit tests (no Spring, no DB).
 */
@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    CustomerRepository customerRepository;
    @Mock
    CustomerMapper customerMapper;

    @InjectMocks
    CustomerService customerService;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    // ==================== Fixtures ====================
    private CustomerRequestDTO dto(String name) {
        return new CustomerRequestDTO(name);
    }

    private Customer customer(UUID id, String name) {
        return Customer.builder().id(id).name(name).build();
    }

    private CustomerResponseDTO response(UUID id, String name) {
        return new CustomerResponseDTO(id, name, null, null); // uprav ak máš iný ctor/fields
    }

    private Page<Customer> pageOf(Customer... customers) {
        return new PageImpl<>(List.of(customers), PageRequest.of(0, 10), customers.length);
    }

    // ==================== Create ====================
    @Nested
    @DisplayName("createCustomer")
    class CreateCustomerTests {

        @Test
        void should_CreateCustomer_whenNameIsUnique() {
            // Given
            CustomerRequestDTO request = dto("Martin");

            Customer mapped = customer(null, "Martin");
            Customer saved = customer(customerId, "Martin");
            CustomerResponseDTO resp = response(customerId, "Martin");

            when(customerRepository.existsByNameIgnoreCase("Martin")).thenReturn(false);
            when(customerMapper.dtoToCustomer(request)).thenReturn(mapped);
            when(customerRepository.save(mapped)).thenReturn(saved);
            when(customerMapper.customerToResponseDto(saved)).thenReturn(resp);

            // When
            CustomerResponseDTO result = customerService.createCustomer(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(customerId);
            assertThat(result.name()).isEqualTo("Martin");

            verify(customerRepository).existsByNameIgnoreCase("Martin");
            verify(customerMapper).dtoToCustomer(request);
            verify(customerRepository).save(mapped);
            verify(customerMapper).customerToResponseDto(saved);
            verifyNoMoreInteractions(customerRepository, customerMapper);
        }

        @Test
        void should_ThrowConflict_whenDuplicateName() {
            // Given
            CustomerRequestDTO request = dto("Martin");
            when(customerRepository.existsByNameIgnoreCase("Martin")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(ResourceAlreadyExistsExceptions.class);

            verify(customerRepository).existsByNameIgnoreCase("Martin");
            verifyNoMoreInteractions(customerRepository, customerMapper);
        }
    }

    // ==================== Get By Id ====================
    @Nested
    @DisplayName("getCustomerById")
    class GetCustomerByIdTests {

        @Test
        void should_ReturnCustomer_whenExists() {
            // Given
            Customer found = customer(customerId, "Martin");
            CustomerResponseDTO resp = response(customerId, "Martin");

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(found));
            when(customerMapper.customerToResponseDto(found)).thenReturn(resp);

            // When
            CustomerResponseDTO result = customerService.getCustomerById(customerId);

            // Then
            assertThat(result.id()).isEqualTo(customerId);
            assertThat(result.name()).isEqualTo("Martin");

            verify(customerRepository).findById(customerId);
            verify(customerMapper).customerToResponseDto(found);
            verifyNoMoreInteractions(customerRepository, customerMapper);
        }

        @Test
        void should_ThrowNotFound_whenMissing() {
            // Given
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                    .isInstanceOf(NotFoundException.class);

            verify(customerRepository).findById(customerId);
            verifyNoMoreInteractions(customerRepository, customerMapper);
        }
    }

    // ==================== Update ====================
    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomerTests {

        @Test
        void should_UpdateCustomer_whenNameChangedAndUnique() {
            // Given
            UUID id = customerId;
            Customer existing = customer(id, "Old");
            CustomerRequestDTO dto = dto("New");

            when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
            when(customerRepository.existsByNameIgnoreCaseAndIdNot("New", id)).thenReturn(false);

            // simulate MapStruct void update: mutate the entity
            doAnswer(invocation -> {
                CustomerRequestDTO req = invocation.getArgument(0);
                Customer target = invocation.getArgument(1);
                target.setName(req.name());
                return null;
            }).when(customerMapper).updateCustomerFromDto(dto, existing);

            CustomerResponseDTO resp = response(id, "New");
            when(customerMapper.customerToResponseDto(existing)).thenReturn(resp);

            // When
            CustomerResponseDTO result = customerService.updateCustomerById(id, dto);

            // Then
            assertThat(result.name()).isEqualTo("New");

            verify(customerRepository).findById(id);
            verify(customerRepository).existsByNameIgnoreCaseAndIdNot("New", id);
            verify(customerMapper).updateCustomerFromDto(dto, existing);
            verify(customerMapper).customerToResponseDto(existing);
            verifyNoMoreInteractions(customerRepository, customerMapper);
        }
    }

    // ==================== List / Filter ====================
    @Nested
    @DisplayName("getAllCustomers")
    class GetAllCustomersTests {

        @Test
        void should_ReturnPage_whenNameFilterProvided() {
            // Given
            Customer c1 = customer(UUID.randomUUID(), "Test Customer 1");
            Customer c2 = customer(UUID.randomUUID(), "Test Customer 2");

            when(customerRepository.findByNameContainingIgnoreCase(eq("test"), any()))
                    .thenReturn(pageOf(c1, c2));

            when(customerMapper.customerToResponseDto(c1)).thenReturn(response(c1.getId(), c1.getName()));
            when(customerMapper.customerToResponseDto(c2)).thenReturn(response(c2.getId(), c2.getName()));

            // When
            Page<CustomerResponseDTO> result = customerService.getAllCustomers("test", PageRequest.of(0, 10));

            // Then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                    .extracting(CustomerResponseDTO::name)
                    .containsExactlyInAnyOrder("Test Customer 1", "Test Customer 2");

            verify(customerRepository).findByNameContainingIgnoreCase(eq("test"), any());
        }
    }

}
