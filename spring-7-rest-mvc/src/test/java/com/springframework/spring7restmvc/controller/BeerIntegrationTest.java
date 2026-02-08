package com.springframework.spring7restmvc.controller;

import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Beer API endpoints.
 *
 * Tests the full stack: Controller -> Service -> Repository -> Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class BeerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BeerRepository beerRepository;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveBeer() throws Exception {
        // Given
        String beerJson = """
            {
              "beerName": "Pilsner Urquell",
              "beerStyle": "LAGER",
              "upc": "123456",
              "quantityOnHand": 100,
              "price": 2.50
            }
            """;

        // When - Create
        MvcResult createResult = mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(beerJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        assertThat(location).isNotNull();

        // Then - Retrieve
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beerName").value("Pilsner Urquell"))
                .andExpect(jsonPath("$.beerStyle").value("LAGER"))
                .andExpect(jsonPath("$.price").value(2.50))
                .andExpect(jsonPath("$.quantityOnHand").value(100))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldPreventDuplicateBeerNames_CaseInsensitive() throws Exception {
        // Given - existing beer
        Beer existing = Beer.builder()
                .beerName("Pilsner Urquell")
                .beerStyle(BeerStyle.LAGER)
                .upc("123456")
                .price(BigDecimal.valueOf(2.50))
                .build();
        beerRepository.save(existing);

        // When - try to create duplicate with different case
        String duplicateJson = """
            {
              "beerName": "PILSNER URQUELL",
              "beerStyle": "IPA",
              "upc": "999999",
              "quantityOnHand": 50,
              "price": 3.00
            }
            """;

        // Then - should fail with conflict
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.detail").value(containsStringIgnoringCase("pilsner")));
    }

    @Test
    void shouldAllowUpdateWithSameName() throws Exception {
        // Given - existing beer
        Beer beer = Beer.builder()
                .beerName("Staropramen")
                .beerStyle(BeerStyle.LAGER)
                .upc("123456")
                .price(BigDecimal.valueOf(2.50))
                .build();
        Beer saved = beerRepository.save(beer);

        // When - update with same name but different price
        String updateJson = """
            {
              "beerName": "Staropramen",
              "beerStyle": "LAGER",
              "upc": "123456",
              "quantityOnHand": 200,
              "price": 3.00
            }
            """;

        // Then - should succeed
        mockMvc.perform(put(BeerController.BASE_URL + "/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beerName").value("Staropramen"))
                .andExpect(jsonPath("$.price").value(3.00))
                .andExpect(jsonPath("$.quantityOnHand").value(200));

        // Verify in database
        Beer updated = beerRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(3.00));
        assertThat(updated.getQuantityOnHand()).isEqualTo(200);
    }

    @Test
    void shouldPreventUpdateWithDuplicateName() throws Exception {
        // Given - two existing beers
        Beer beer1 = beerRepository.save(Beer.builder()
                .beerName("Zlaty Bazant")
                .beerStyle(BeerStyle.LAGER)
                .upc("111111")
                .price(BigDecimal.valueOf(1.50))
                .build());

        beerRepository.save(Beer.builder()
                .beerName("Kozel")
                .beerStyle(BeerStyle.LAGER)
                .upc("222222")
                .price(BigDecimal.valueOf(1.80))
                .build());

        // When - try to rename beer1 to beer2's name
        String updateJson = """
            {
              "beerName": "Kozel",
              "beerStyle": "LAGER",
              "upc": "111111",
              "quantityOnHand": 100,
              "price": 1.50
            }
            """;

        // Then - should fail
        mockMvc.perform(put(BeerController.BASE_URL + "/" + beer1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));
    }

    @Test
    void shouldPatchPartialFields() throws Exception {
        // Given - existing beer
        Beer beer = beerRepository.save(Beer.builder()
                .beerName("Corgoň")
                .beerStyle(BeerStyle.LAGER)
                .upc("333333")
                .quantityOnHand(100)
                .price(BigDecimal.valueOf(1.90))
                .build());

        // When - patch only price and quantity
        String patchJson = """
            {
              "quantityOnHand": 150,
              "price": 2.20
            }
            """;

        // Then - should update only those fields
        mockMvc.perform(patch(BeerController.BASE_URL + "/" + beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beerName").value("Corgoň"))
                .andExpect(jsonPath("$.beerStyle").value("LAGER"))
                .andExpect(jsonPath("$.upc").value("333333"))
                .andExpect(jsonPath("$.quantityOnHand").value(150))
                .andExpect(jsonPath("$.price").value(2.20));

        // Verify in database
        Beer updated = beerRepository.findById(beer.getId()).orElseThrow();
        assertThat(updated.getBeerName()).isEqualTo("Corgoň");
        assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2.20));
    }

    @Test
    void shouldAllowPatchWithSameName() throws Exception {
        // Given
        Beer beer = beerRepository.save(Beer.builder()
                .beerName("Šariš")
                .beerStyle(BeerStyle.LAGER)
                .upc("444444")
                .price(BigDecimal.valueOf(1.70))
                .build());

        // When - patch with same name but different price
        String patchJson = """
            {
              "beerName": "Šariš",
              "price": 2.00
            }
            """;

        // Then - should succeed
        mockMvc.perform(patch(BeerController.BASE_URL + "/" + beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(2.00));
    }

    @Test
    void shouldDeleteBeer() throws Exception {
        // Given
        Beer beer = beerRepository.save(Beer.builder()
                .beerName("Steiger")
                .beerStyle(BeerStyle.LAGER)
                .upc("555555")
                .price(BigDecimal.valueOf(1.40))
                .build());

        // When
        mockMvc.perform(delete(BeerController.BASE_URL + "/" + beer.getId()))
                .andExpect(status().isNoContent());

        // Then - verify deletion
        assertThat(beerRepository.findById(beer.getId())).isEmpty();
    }

    @Test
    void shouldReturn404ForNonExistentBeer() throws Exception {
        // Given
        String randomId = "123e4567-e89b-12d3-a456-426614174000";

        // When/Then
        mockMvc.perform(get(BeerController.BASE_URL + "/" + randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value(containsString("Beer not found")));
    }

    @Test
    void shouldValidateRequiredFields() throws Exception {
        // Given - missing required fields
        String invalidJson = """
            {
              "beerName": "",
              "upc": ""
            }
            """;

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.beerName").exists())
                .andExpect(jsonPath("$.errors.beerStyle").exists())
                .andExpect(jsonPath("$.errors.upc").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void shouldValidatePositivePrice() throws Exception {
        // Given - negative price
        String invalidJson = """
            {
              "beerName": "Test Beer",
              "beerStyle": "LAGER",
              "upc": "123456",
              "quantityOnHand": 100,
              "price": -1.50
            }
            """;

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void shouldGetAllBeers() throws Exception {
        // Given - multiple beers
        beerRepository.save(Beer.builder()
                .beerName("Beer 1")
                .beerStyle(BeerStyle.LAGER)
                .upc("111")
                .price(BigDecimal.valueOf(1.50))
                .build());

        beerRepository.save(Beer.builder()
                .beerName("Beer 2")
                .beerStyle(BeerStyle.IPA)
                .upc("222")
                .price(BigDecimal.valueOf(2.50))
                .build());

        beerRepository.save(Beer.builder()
                .beerName("Beer 3")
                .beerStyle(BeerStyle.STOUT)
                .upc("333")
                .price(BigDecimal.valueOf(3.50))
                .build());

        // When/Then
        mockMvc.perform(get(BeerController.BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].beerName", containsInAnyOrder("Beer 1", "Beer 2", "Beer 3")));
    }



    @Test
    void shouldHandleMalformedJson() throws Exception {
        // Given - invalid JSON
        String malformedJson = "{ beerName: 'invalid' }";

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid JSON input"));
    }

    @Test
    void shouldUpdateTimestampsOnUpdate() throws Exception {
        // Given
        Beer beer = beerRepository.save(Beer.builder()
                .beerName("Time Test Beer")
                .beerStyle(BeerStyle.LAGER)
                .upc("TIME123")
                .price(BigDecimal.valueOf(2.00))
                .build());

        var originalUpdatedAt = beer.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(100);

        // When
        String updateJson = """
            {
              "beerName": "Time Test Beer",
              "beerStyle": "IPA",
              "upc": "TIME123",
              "quantityOnHand": 100,
              "price": 2.50
            }
            """;

        mockMvc.perform(put(BeerController.BASE_URL + "/" + beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        // Then - verify updatedAt changed
        Beer updated = beerRepository.findById(beer.getId()).orElseThrow();
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getCreatedAt()).isEqualTo(beer.getCreatedAt());
    }
}