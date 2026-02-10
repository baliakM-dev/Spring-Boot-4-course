package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.entities.Customer;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomerMapperTest {

    private final CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void shouldMapCustomerToResponseDTO() {
        Customer customer = Customer.builder().name("Martin").build();

        CustomerResponseDTO dto = customerMapper.customerToResponseDto(customer);

        assertThat(dto.name()).isEqualTo("Martin");
    }

}
