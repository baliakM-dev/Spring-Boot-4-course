package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.customer.CustomerRequestDTO;
import com.springframework.spring7restmvc.dto.customer.CustomerResponseDTO;
import com.springframework.spring7restmvc.entities.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for Customer entity and DTOs.
 * Component model Spring ensures CDI injection.
 */
@Mapper
public interface CustomerMapper {

    /**
     * Convert Customer entity to response DTO.
     *
     * @param customer customer entity
     * @return CustomerResponseDTO response DTO
     */
    CustomerResponseDTO customerToResponseDto(Customer customer);

    /**
     * Convert request DTO to a Customer entity.
     *
     * @param customerRequestDTO customer request DTO
     * @return Customer entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer dtoToCustomer(CustomerRequestDTO customerRequestDTO);

    /**
     * Update the existing Customer entity from request DTO.
     *
     * @param dto customer request DTO
     * @param customer existing Customer entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateCustomerFromDto(CustomerRequestDTO dto, @MappingTarget Customer customer);
}
