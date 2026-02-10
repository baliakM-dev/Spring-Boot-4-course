package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.entities.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface CustomerMapper {

    // Entity -> Response DTO
    CustomerResponseDTO customerToResponseDto(Customer customer);

    // Request DTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer dtoToCustomer(CustomerRequestDTO customerRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateCustomerFromDto(CustomerRequestDTO dto, @MappingTarget Customer customer);
}
