package com.springframework.spring7restmvc.controller;

import com.springframework.spring7restmvc.dto.beer.BeerRequestDTO;
import com.springframework.spring7restmvc.dto.beer.BeerResponseDTO;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.exceptions.NotFoundException;
import com.springframework.spring7restmvc.exceptions.ResourceAlreadyExistsExceptions;
import com.springframework.spring7restmvc.services.BeerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BeerController.
 *
 * Uses MockMvc to test REST endpoints with mocked service layer.
 */
@WebMvcTest(BeerController.class)
class BeerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BeerService beerService;

    @Test
    void testCreateBeerSuccess() throws Exception {
        // Given
        BeerRequestDTO request = new BeerRequestDTO(
                "New Beer",
                BeerStyle.IPA,
                "123456",
                100,
                new BigDecimal("10.50")
        );

        UUID beerId = UUID.randomUUID();
        BeerResponseDTO response = new BeerResponseDTO(
                beerId,
                request.beerName(),
                request.beerStyle().name(),
                request.upc(),
                request.quantityOnHand(),
                request.price(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(beerService.createNewBeer(any(BeerRequestDTO.class)))
                .willReturn(response);

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString(beerId.toString())));

        then(beerService).should().createNewBeer(any(BeerRequestDTO.class));
    }

    @Test
    void testCreateBeerWithObjectPriceFails() throws Exception {
        // Given - invalid price format (object instead of number)
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "Test Beer");
        beerMap.put("beerStyle", "IPA");
        beerMap.put("upc", "123456");

        Map<String, Object> priceMap = new HashMap<>();
        priceMap.put("amount", 10.50);
        beerMap.put("price", priceMap);

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid JSON input"))
                .andExpect(jsonPath("$.detail").value("The provided JSON is malformed or has invalid data types."));
    }

    @Test
    void testCreateBeerValidationFails() throws Exception {
        // Given - multiple validation errors
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "");
        beerMap.put("beerStyle", null);
        beerMap.put("upc", "");
        beerMap.put("price", -2.5);

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.beerName").exists())
                .andExpect(jsonPath("$.errors.beerStyle").exists())
                .andExpect(jsonPath("$.errors.upc").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void testCreateBeerWithNameAlreadyExists() throws Exception {
        // Given
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "Duplicate Beer");
        beerMap.put("beerStyle", "ALE");
        beerMap.put("upc", "123");
        beerMap.put("quantityOnHand", 10);
        beerMap.put("price", 2.5);

        given(beerService.createNewBeer(any(BeerRequestDTO.class)))
                .willThrow(new ResourceAlreadyExistsExceptions("Beer", "beerName", "Duplicate Beer"));

        // When/Then
        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testGetBeerByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        given(beerService.getBeerById(nonExistentId))
                .willThrow(new NotFoundException("Beer", "id", nonExistentId.toString()));

        // When/Then
        mockMvc.perform(get(BeerController.BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value(containsString("Beer not found")))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testGetBeerByIdSuccess() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();
        BeerResponseDTO mockResponse = new BeerResponseDTO(
                beerId,
                "Pilsner Urquell",
                "LAGER",
                "012345678905",
                100,
                new BigDecimal("2.99"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(beerService.getBeerById(beerId))
                .willReturn(mockResponse);

        // When/Then
        mockMvc.perform(get(BeerController.BASE_URL + "/" + beerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beerId.toString()))
                .andExpect(jsonPath("$.beerName").value("Pilsner Urquell"))
                .andExpect(jsonPath("$.price").value(2.99));

        then(beerService).should().getBeerById(beerId);
    }

    @Test
    void testGetAllBeersSuccess() throws Exception {
        // Given
        List<BeerResponseDTO> beers = List.of(
                new BeerResponseDTO(
                        UUID.randomUUID(),
                        "Beer 1",
                        "LAGER",
                        "111",
                        100,
                        new BigDecimal("1.50"),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ),
                new BeerResponseDTO(
                        UUID.randomUUID(),
                        "Beer 2",
                        "IPA",
                        "222",
                        200,
                        new BigDecimal("2.50"),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );

        given(beerService.getAllBeers())
                .willReturn(beers);

        // When/Then
        mockMvc.perform(get(BeerController.BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].beerName").value("Beer 1"))
                .andExpect(jsonPath("$[1].beerName").value("Beer 2"));
    }

    @Test
    void testUpdateBeerByIdMethodNotAllowed() throws Exception {
        // Given
        UUID id = UUID.randomUUID();

        // When/Then - POST to update endpoint should fail
        mockMvc.perform(post(BeerController.BASE_URL + "/" + id))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testUpdateBeerByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        String body = """
                {
                  "beerName": "IPA",
                  "beerStyle": "ALE",
                  "upc": "123",
                  "quantityOnHand": 0,
                  "price": 1
                }
                """;

        given(beerService.updateBeerById(eq(nonExistentId), any(BeerRequestDTO.class)))
                .willThrow(new NotFoundException("Beer", "id", nonExistentId.toString()));

        // When/Then
        mockMvc.perform(put(BeerController.BASE_URL + "/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBeerByIdSuccess() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();
        BeerRequestDTO request = new BeerRequestDTO(
                "Updated Beer",
                BeerStyle.IPA,
                "123456",
                100,
                new BigDecimal("12.50")
        );

        BeerResponseDTO response = new BeerResponseDTO(
                beerId,
                request.beerName(),
                request.beerStyle().name(),
                request.upc(),
                request.quantityOnHand(),
                request.price(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(beerService.updateBeerById(eq(beerId), any(BeerRequestDTO.class)))
                .willReturn(response);

        // When/Then
        mockMvc.perform(put(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beerId.toString()))
                .andExpect(jsonPath("$.beerName").value("Updated Beer"));

        then(beerService).should().updateBeerById(eq(beerId), any(BeerRequestDTO.class));
    }

    @Test
    void testUpdateBeerValidationFails() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "");
        beerMap.put("beerStyle", "IPA");
        beerMap.put("upc", "123456");
        beerMap.put("price", 10.50);

        // When/Then
        mockMvc.perform(put(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.beerName").exists());
    }

    @Test
    void testPatchBeerByIdSuccess() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();
        Map<String, Object> patchMap = new HashMap<>();
        patchMap.put("beerName", "Patched Name");

        BeerResponseDTO response = new BeerResponseDTO(
                beerId,
                "Patched Name",
                BeerStyle.IPA.name(),
                "123456",
                100,
                new BigDecimal("12.50"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(beerService.patchBeerById(eq(beerId), any(BeerRequestDTO.class)))
                .willReturn(response);

        // When/Then
        mockMvc.perform(patch(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beerId.toString()))
                .andExpect(jsonPath("$.beerName").value("Patched Name"));

        then(beerService).should().patchBeerById(eq(beerId), any(BeerRequestDTO.class));
    }

    @Test
    void testPatchBeerByIdNotFound() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();
        Map<String, Object> patchMap = new HashMap<>();
        patchMap.put("price", 5.00);

        given(beerService.patchBeerById(eq(beerId), any(BeerRequestDTO.class)))
                .willThrow(new NotFoundException("Beer", "id", beerId.toString()));

        // When/Then
        mockMvc.perform(patch(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchMap)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteBeerByIdSuccess() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();

        willDoNothing().given(beerService).deleteBeerById(beerId);

        // When/Then
        mockMvc.perform(delete(BeerController.BASE_URL + "/" + beerId))
                .andExpect(status().isNoContent());

        then(beerService).should().deleteBeerById(beerId);
    }

    @Test
    void testDeleteBeerByIdNotFound() throws Exception {
        // Given
        UUID beerId = UUID.randomUUID();

        willThrow(new NotFoundException("Beer", "id", beerId.toString()))
                .given(beerService).deleteBeerById(beerId);

        // When/Then
        mockMvc.perform(delete(BeerController.BASE_URL + "/" + beerId))
                .andExpect(status().isNotFound());

        then(beerService).should().deleteBeerById(beerId);
    }
}