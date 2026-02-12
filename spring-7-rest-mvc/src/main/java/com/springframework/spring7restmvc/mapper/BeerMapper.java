package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.Beer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for Beer entity and DTOs.
 * Component model Spring ensures CDI injection.
 */
@Mapper
public interface BeerMapper {

    /**
     * Convert Beer entity to response DTO.
     *
     * @param beer beer entity
     * @return BeerResponseDTO response DTO
     */
    BeerResponseDTO beerToResponseDTO(Beer beer);

    /**
     * Convert request DTO to Beer entity.
     *
     * @param beerRequestDTO beer request DTO
     * @return Beer entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "categories", ignore = true)
    Beer dtoToBeer(BeerRequestDTO beerRequestDTO);

    /**
     * Update the existing Beer entity from request DTO.
     *
     * @param dto beer request DTO
     * @param beer existing Beer entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateBeerFromDto(BeerRequestDTO dto, @MappingTarget Beer beer);

}
