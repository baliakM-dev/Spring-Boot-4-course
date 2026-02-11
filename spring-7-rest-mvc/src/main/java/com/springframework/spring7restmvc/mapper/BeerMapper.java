package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.Beer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface BeerMapper {

    /**
     * Convert Beer entity to response DTO.
     * Categories are automatically mapped via CategoryMapper.
     */
    BeerResponseDTO beerToResponseDTO(Beer beer);

    /**
     * Convert request DTO to Beer entity.
     * Ignores managed fields and categories (managed separately).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "categories", ignore = true)
    Beer dtoToBeer(BeerRequestDTO beerRequestDTO);

    /**
     * Update the existing Beer entity from request DTO.
     * Ignores managed fields and categories (managed via dedicated service methods).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateBeerFromDto(BeerRequestDTO dto, @MappingTarget Beer beer);

}
