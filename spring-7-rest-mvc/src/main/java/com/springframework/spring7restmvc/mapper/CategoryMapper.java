package com.springframework.spring7restmvc.mapper;

import com.springframework.spring7restmvc.dto.category.CategoryRequestDTO;
import com.springframework.spring7restmvc.dto.category.CategoryResponseDTO;
import com.springframework.spring7restmvc.entities.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for Category entity and DTOs.
 * Component model Spring ensures CDI injection.
 */
@Mapper
public interface CategoryMapper {

    /**
     * Convert Category entity to response DTO.
     * Excludes beers collection to prevent circular references.
     */
    CategoryResponseDTO categoryToResponseDTO(Category category);

    /**
     * Convert request DTO to Category entity.
     * Ignores managed fields (id, version, timestamps, beers).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "beers", ignore = true)
    Category dtoToCategory(CategoryRequestDTO dto);

    /**
     * Update the existing Category entity from request DTO.
     * Ignores managed fields and beer collection.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "beers", ignore = true)
    void updateCategoryFromDto(CategoryRequestDTO dto, @MappingTarget Category category);
}