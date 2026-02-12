package com.springframework.spring7restmvc.services;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Category;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.mapper.BeerMapper;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CategoryRepository;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BeerService}.
 * <p>
 * Focus:
 * - service branching logic (filters)
 * - uniqueness validation
 * - category association validation
 * - not found / conflict exceptions
 * - inventory masking (showInventoryOnHand = false)
 * <p>
 * Note: These are pure unit tests (no Spring, no DB).
 */
@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    @Mock
    BeerRepository beerRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    BeerMapper beerMapper;
    @Mock
    CategoryService categoryService;
    @InjectMocks
    BeerService beerService;

    private UUID beerId;
    private UUID cat1Id;
    private UUID cat2Id;

    private Beer existingBeer;
    private BeerResponseDTO responseDto;

    @BeforeEach
    void setUp() {
        beerId = UUID.randomUUID();
        cat1Id = UUID.randomUUID();
        cat2Id = UUID.randomUUID();

        existingBeer = Beer.builder()
                .id(beerId)
                .beerName("Pilsner Urquell")
                .beerStyle(BeerStyle.LAGER)
                .upc("123456")
                .quantityOnHand(100)
                .price(new BigDecimal("2.50"))
                .categories(new HashSet<>())
                .build();

        responseDto = new BeerResponseDTO(
                beerId,
                "Pilsner Urquell",
                BeerStyle.LAGER.toString(),
                "123456",
                100,
                new BigDecimal("2.50"),
                null,
                null,
                null
        );
    }

    // ==================== Test Fixtures ====================
    private BeerRequestDTO createDto(String name, BeerStyle style, String upc, Integer qoh, BigDecimal price, Set<UUID> categoryIds) {
        return new BeerRequestDTO(
                name,
                style,
                upc,
                qoh,
                price,
                categoryIds
        );
    }

    private Page<Beer> pageOf(Beer... beers) {
        return new PageImpl<>(Arrays.asList(beers), PageRequest.of(0, 10), beers.length);
    }

    // ==================== Create Beer Tests ====================
    @Nested
    @DisplayName("createNewBeer")
    class CreateNewBeerTests {

        @Test
        void shouldCreateBeer_withoutCategories() {
            // Given
            BeerRequestDTO dto = createDto("New Beer", BeerStyle.IPA, "UPC1", 10, new BigDecimal("3.00"), null);

            Beer mapped = Beer.builder()
                    .beerName("New Beer")
                    .beerStyle(BeerStyle.IPA)
                    .upc("UPC1")
                    .quantityOnHand(10)
                    .price(new BigDecimal("3.00"))
                    .categories(new HashSet<>())
                    .build();

            Beer saved = Beer.builder()
                    .id(UUID.randomUUID())
                    .beerName("New Beer")
                    .beerStyle(BeerStyle.IPA)
                    .upc("UPC1")
                    .quantityOnHand(10)
                    .price(new BigDecimal("3.00"))
                    .categories(new HashSet<>())
                    .build();

            BeerResponseDTO out = new BeerResponseDTO(
                    saved.getId(),
                    "New Beer",
                    BeerStyle.IPA.toString(),
                    "UPC1",
                    10,
                    new BigDecimal("3.00"),
                    null,
                    null,
                    null
            );

            // When
            when(beerRepository.existsByBeerNameIgnoreCase("New Beer")).thenReturn(false);
            when(beerMapper.dtoToBeer(dto)).thenReturn(mapped);
            when(beerRepository.save(mapped)).thenReturn(saved);
            when(beerMapper.beerToResponseDTO(saved)).thenReturn(out);

            BeerResponseDTO result = beerService.createNewBeer(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.beerName()).isEqualTo("New Beer");

            // Verify
            verify(beerRepository).existsByBeerNameIgnoreCase("New Beer");
            verify(categoryRepository, never()).findAllById(anyCollection());
            verify(beerRepository).save(mapped);
            verify(beerMapper).beerToResponseDTO(saved);
        }
    }

    @Test
    void shouldThrowConflict_whenDuplicateName() {
        BeerRequestDTO dto = createDto("Dupe", BeerStyle.LAGER, "U", 1, new BigDecimal("1.00"), null);

        when(beerRepository.existsByBeerNameIgnoreCase("Dupe")).thenReturn(true);

        assertThatThrownBy(() -> beerService.createNewBeer(dto))
                .isInstanceOf(ResourceAlreadyExistsExceptions.class);

        verify(beerRepository).existsByBeerNameIgnoreCase("Dupe");
        verifyNoInteractions(beerMapper);
        verify(beerRepository, never()).save(any());
    }

    @Test
    void shouldAssociateCategories_whenProvided_andAllExist() {
        Category c1 = Category.builder().id(cat1Id).description("Lager").beers(new HashSet<>()).build();
        Category c2 = Category.builder().id(cat2Id).description("Pilsner").beers(new HashSet<>()).build();

        Set<UUID> ids = Set.of(cat1Id, cat2Id);
        BeerRequestDTO dto = createDto("Beer", BeerStyle.LAGER, "U", 1, new BigDecimal("1.00"), ids);

        Beer mapped = Beer.builder()
                .beerName("Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("U")
                .quantityOnHand(1)
                .price(new BigDecimal("1.00"))
                .categories(new HashSet<>())
                .build();

        Beer saved = Beer.builder()
                .id(UUID.randomUUID())
                .beerName("Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("U")
                .quantityOnHand(1)
                .price(new BigDecimal("1.00"))
                .categories(new HashSet<>())
                .build();

        BeerResponseDTO out = new BeerResponseDTO(
                saved.getId(), "Beer", BeerStyle.LAGER.toString(), "U", 1, new BigDecimal("1.00"),
                null, null, null
        );

        when(beerRepository.existsByBeerNameIgnoreCase("Beer")).thenReturn(false);
        when(beerMapper.dtoToBeer(dto)).thenReturn(mapped);
        when(categoryRepository.findAllById(ids)).thenReturn(List.of(c1, c2));
        when(beerRepository.save(mapped)).thenReturn(saved);
        when(beerMapper.beerToResponseDTO(saved)).thenReturn(out);

        BeerResponseDTO result = beerService.createNewBeer(dto);

        assertThat(result).isNotNull();
        assertThat(mapped.getCategories()).hasSize(2); // categories were added via beer.addCategory

        verify(categoryRepository).findAllById(ids);
        verify(beerRepository).save(mapped);
    }

    @Test
    void shouldThrowNotFound_whenAnyCategoryMissing() {
        Category c1 = Category.builder().id(cat1Id).description("Lager").beers(new HashSet<>()).build();

        Set<UUID> ids = Set.of(cat1Id, cat2Id);
        BeerRequestDTO dto = createDto("Beer", BeerStyle.LAGER, "U", 1, new BigDecimal("1.00"), ids);

        Beer mapped = Beer.builder()
                .beerName("Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("U")
                .quantityOnHand(1)
                .price(new BigDecimal("1.00"))
                .categories(new HashSet<>())
                .build();

        when(beerRepository.existsByBeerNameIgnoreCase("Beer")).thenReturn(false);
        when(beerMapper.dtoToBeer(dto)).thenReturn(mapped);
        when(categoryRepository.findAllById(ids)).thenReturn(List.of(c1)); // missing cat2

        assertThatThrownBy(() -> beerService.createNewBeer(dto))
                .isInstanceOf(NotFoundException.class);

        verify(categoryRepository).findAllById(ids);
        verify(beerRepository, never()).save(any());
    }

    // ==================== Query Beer Tests ====================
    @Nested
    @DisplayName("getAllBeers")
    class GetAllBeersTests {

        @Test
        void shouldQueryByNameOnly_whenBeerNameProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(beerRepository.findAllByBeerNameContainingIgnoreCase("pil", pageable)).thenReturn(pageOf(existingBeer));
            when(beerMapper.beerToResponseDTO(any(Beer.class))).thenReturn(responseDto);

            Page<BeerResponseDTO> result = beerService.getAllBeers("pil", null, true, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(beerRepository).findAllByBeerNameContainingIgnoreCase("pil", pageable);
            verify(beerRepository, never()).findAllByBeerStyle(any(), any());
            verify(beerRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        void shouldQueryByStyleOnly_whenStyleProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(beerRepository.findAllByBeerStyle(BeerStyle.LAGER, pageable)).thenReturn(pageOf(existingBeer));
            when(beerMapper.beerToResponseDTO(any(Beer.class))).thenReturn(responseDto);

            Page<BeerResponseDTO> result = beerService.getAllBeers(null, BeerStyle.LAGER, true, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(beerRepository).findAllByBeerStyle(BeerStyle.LAGER, pageable);
            verify(beerRepository, never()).findAllByBeerNameContainingIgnoreCase(anyString(), any());
        }

        @Test
        void shouldQueryByNameAndStyle_whenBothProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(beerRepository.findAllByBeerNameContainingIgnoreCaseAndBeerStyle("pil", BeerStyle.LAGER, pageable))
                    .thenReturn(pageOf(existingBeer));
            when(beerMapper.beerToResponseDTO(any(Beer.class))).thenReturn(responseDto);

            Page<BeerResponseDTO> result = beerService.getAllBeers("pil", BeerStyle.LAGER, true, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(beerRepository).findAllByBeerNameContainingIgnoreCaseAndBeerStyle("pil", BeerStyle.LAGER, pageable);
        }

        @Test
        void shouldQueryAll_whenNoFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            when(beerRepository.findAll(pageable)).thenReturn(pageOf(existingBeer));
            when(beerMapper.beerToResponseDTO(any(Beer.class))).thenReturn(responseDto);

            Page<BeerResponseDTO> result = beerService.getAllBeers(null, null, true, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(beerRepository).findAll(pageable);
        }

        @Test
        void shouldHideInventory_whenShowInventoryOnHandIsFalse() {
            Pageable pageable = PageRequest.of(0, 10);
            Beer beerWithInventory = Beer.builder()
                    .id(beerId)
                    .beerName("Inv")
                    .beerStyle(BeerStyle.LAGER)
                    .quantityOnHand(123)
                    .price(new BigDecimal("1.00"))
                    .categories(new HashSet<>())
                    .build();

            when(beerRepository.findAll(pageable)).thenReturn(pageOf(beerWithInventory));
            when(beerMapper.beerToResponseDTO(any(Beer.class))).thenReturn(responseDto);

            beerService.getAllBeers(null, null, false, pageable);

            // service mutates entities in the page before mapping
            assertThat(beerWithInventory.getQuantityOnHand()).isNull();
        }
    }
    // ==================== Get BeerById ====================
    @Nested
    @DisplayName("getBeerById")
    class GetBeerByIdTests {

        @Test
        void shouldReturnBeer_whenFound() {
            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(beerMapper.beerToResponseDTO(existingBeer)).thenReturn(responseDto);

            BeerResponseDTO result = beerService.getBeerById(beerId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(beerId);

            verify(beerRepository).findByIdWithCategories(beerId);
        }

        @Test
        void shouldThrowNotFound_whenMissing() {
            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> beerService.getBeerById(beerId))
                    .isInstanceOf(NotFoundException.class);

            verify(beerRepository).findByIdWithCategories(beerId);
            verifyNoInteractions(beerMapper);
        }
    }

    // ==================== Update Beer Tests ====================
    @Nested
    @DisplayName("updateBeerById")
    class UpdateBeerByIdTests {

        @Test
        void shouldUpdate_withoutUniquenessCheck_whenNameUnchangedIgnoreCase() {
            BeerRequestDTO dto = createDto("PILsner URQuell", BeerStyle.IPA, "123456", 200, new BigDecimal("3.00"), null);

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(beerRepository.saveAndFlush(existingBeer)).thenReturn(existingBeer);
            when(beerMapper.beerToResponseDTO(existingBeer)).thenReturn(responseDto);

            BeerResponseDTO result = beerService.updateBeerById(beerId, dto);

            assertThat(result).isNotNull();

            // uniqueness methods must not be called
            verify(beerRepository, never()).existsByBeerNameIgnoreCaseAndIdNot(anyString(), any());
            verify(beerRepository, never()).existsByBeerNameIgnoreCase(anyString());

            verify(beerMapper).updateBeerFromDto(dto, existingBeer);
            verify(beerRepository).saveAndFlush(existingBeer);
        }

        @Test
        void shouldThrowConflict_whenNameChangedAndDuplicateExists() {
            BeerRequestDTO dto = createDto("New Name", BeerStyle.IPA, "U", 1, new BigDecimal("1.00"), null);

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(beerRepository.existsByBeerNameIgnoreCaseAndIdNot("New Name", beerId)).thenReturn(true);

            assertThatThrownBy(() -> beerService.updateBeerById(beerId, dto))
                    .isInstanceOf(ResourceAlreadyExistsExceptions.class);

            verify(beerRepository).existsByBeerNameIgnoreCaseAndIdNot("New Name", beerId);
            verify(beerMapper, never()).updateBeerFromDto(any(), any());
            verify(beerRepository, never()).saveAndFlush(any());
        }

        @Test
        void shouldThrowNotFound_whenBeerMissing() {
            BeerRequestDTO dto = createDto("X", BeerStyle.LAGER, "U", 1, new BigDecimal("1.00"), null);

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> beerService.updateBeerById(beerId, dto))
                    .isInstanceOf(NotFoundException.class);

            verify(beerRepository).findByIdWithCategories(beerId);
            verifyNoMoreInteractions(beerRepository);
        }
    }

    // ==================== Patch Beer Tests ====================
    @Nested
    @DisplayName("patchBeerById")
    class PatchBeerByIdTests {

        @Test
        void shouldPatchOnlyPrice_whenOnlyPriceProvided() {
            BeerRequestDTO patch = createDto(null, null, null, null, new BigDecimal("9.99"), null);

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(beerMapper.beerToResponseDTO(existingBeer)).thenReturn(responseDto);

            beerService.patchBeerById(beerId, patch);

            assertThat(existingBeer.getPrice()).isEqualByComparingTo("9.99");
            // unchanged fields
            assertThat(existingBeer.getBeerName()).isEqualTo("Pilsner Urquell");
            assertThat(existingBeer.getUpc()).isEqualTo("123456");

            verify(beerRepository, never()).existsByBeerNameIgnoreCaseAndIdNot(anyString(), any());
        }

        @Test
        void shouldThrowConflict_whenPatchNameDuplicate() {
            BeerRequestDTO patch = createDto("New Name", null, null, null, null, null);

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(beerRepository.existsByBeerNameIgnoreCaseAndIdNot("New Name", beerId)).thenReturn(true);

            assertThatThrownBy(() -> beerService.patchBeerById(beerId, patch))
                    .isInstanceOf(ResourceAlreadyExistsExceptions.class);

            verify(beerRepository).existsByBeerNameIgnoreCaseAndIdNot("New Name", beerId);
        }
    }

    // ==================== Delete Beer Tests ====================
    @Nested
    @DisplayName("deleteBeerById")
    class DeleteBeerByIdTests {

        @Test
        void shouldDelete_whenFound() {
            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));

            beerService.deleteBeerById(beerId);

            verify(beerRepository).delete(existingBeer);
        }

        @Test
        void shouldThrowNotFound_whenMissing() {
            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> beerService.deleteBeerById(beerId))
                    .isInstanceOf(NotFoundException.class);

            verify(beerRepository, never()).delete(any());
        }
    }
    // ==================== Categories management Tests ====================
    @Nested
    @DisplayName("category operations")
    class CategoryOperationsTests {

        @Test
        void shouldAddCategoryToBeer() {
            Category cat = Category.builder().id(cat1Id).description("Lager").beers(new HashSet<>()).build();

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(categoryService.getCategoryOrThrow(cat1Id)).thenReturn(cat);
            when(beerMapper.beerToResponseDTO(existingBeer)).thenReturn(responseDto);

            beerService.addCategoryToBeer(beerId, cat1Id);

            assertThat(existingBeer.getCategories()).contains(cat);
            assertThat(cat.getBeers()).contains(existingBeer);
        }

        @Test
        void shouldRemoveCategoryFromBeer() {
            Category cat = Category.builder().id(cat1Id).description("Lager").beers(new HashSet<>()).build();
            existingBeer.addCategory(cat);

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(categoryService.getCategoryOrThrow(cat1Id)).thenReturn(cat);
            when(beerMapper.beerToResponseDTO(existingBeer)).thenReturn(responseDto);

            beerService.removeCategoryFromBeer(beerId, cat1Id);

            assertThat(existingBeer.getCategories()).doesNotContain(cat);
            assertThat(cat.getBeers()).doesNotContain(existingBeer);
        }

        @Test
        void shouldSetCategoriesForBeer_replacingExistingOnes() {
            Category oldCat = Category.builder().id(UUID.randomUUID()).description("Old").beers(new HashSet<>()).build();
            existingBeer.addCategory(oldCat);

            Category new1 = Category.builder().id(cat1Id).description("New1").beers(new HashSet<>()).build();
            Category new2 = Category.builder().id(cat2Id).description("New2").beers(new HashSet<>()).build();

            when(beerRepository.findByIdWithCategories(beerId)).thenReturn(Optional.of(existingBeer));
            when(categoryService.getCategoryOrThrow(cat1Id)).thenReturn(new1);
            when(categoryService.getCategoryOrThrow(cat2Id)).thenReturn(new2);
            when(beerMapper.beerToResponseDTO(existingBeer)).thenReturn(responseDto);

            beerService.setCategoriesForBeer(beerId, Set.of(cat1Id, cat2Id));

            assertThat(existingBeer.getCategories())
                    .containsExactlyInAnyOrder(new1, new2);

            assertThat(oldCat.getBeers()).doesNotContain(existingBeer);
            assertThat(new1.getBeers()).contains(existingBeer);
            assertThat(new2.getBeers()).contains(existingBeer);

            verify(categoryService).getCategoryOrThrow(cat1Id);
            verify(categoryService).getCategoryOrThrow(cat2Id);
        }
    }
}
