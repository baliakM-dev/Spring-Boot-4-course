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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerController.class)
class BeerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    BeerService beerService;

    @Test
    void testCreateBeerWithObjectPriceFails() throws Exception {
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "Test Beer");
        beerMap.put("beerStyle", "IPA");
        beerMap.put("upc", "123456");

        Map<String, Object> priceMap = new HashMap<>();
        priceMap.put("amount", 10.50);
        beerMap.put("price", priceMap);

        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid JSON input"))
                .andExpect(jsonPath("$.detail").value("The provided JSON is malformed or has invalid data types."));
    }

    @Test
    void testCreateBeerValidationFails() throws Exception {
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "");
        beerMap.put("beerStyle", null);
        beerMap.put("upc", "");
        beerMap.put("price", -2.5); // alebo 0 - obe zlyhajú pri @Positive

        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.beerName").exists())
                .andExpect(jsonPath("$.errors.beerStyle").exists())
                .andExpect(jsonPath("$.errors.upc").exists())
                .andExpect(jsonPath("$.errors.price").value("musí byť > 0"));
    }

    @Test
    void testCreateBeerWithNameAlreadyExists() throws Exception {
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "New Beer");
        beerMap.put("beerStyle", "ALE");
        beerMap.put("upc", "123");
        beerMap.put("quantityOnHand", 10);
        beerMap.put("price", 2.5);

        // Použite konštruktor s jedným parametrom
        given(beerService.createNewBeer(any(BeerRequestDTO.class)))
                .willThrow(new ResourceAlreadyExistsExceptions("Beer name already exists"));

        mockMvc.perform(post(BeerController.BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.detail").value("Beer name already exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testGetBeerByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        given(beerService.getBeerById(nonExistentId))
                .willThrow(new NotFoundException("Beer", "id", nonExistentId.toString()));

        mockMvc.perform(get(BeerController.BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value(containsString("Beer not found")))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testGetBeerByIdSuccess() throws Exception {
        UUID beerId = UUID.randomUUID();
        BeerResponseDTO mockResponse = new BeerResponseDTO(
                beerId,
                "Pilsner Urquell",
                "Pilsner",
                "012345678905",
                100,
                new BigDecimal("2.99"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(beerService.getBeerById(beerId))
                .willReturn(mockResponse);

        mockMvc.perform(get(BeerController.BASE_URL + "/" + beerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beerId.toString()))
                .andExpect(jsonPath("$.beerName").value("Pilsner Urquell"))
                .andExpect(jsonPath("$.price").value(2.99));
    }

    @Test
    void testGetAllBeersSuccess() throws Exception {
        mockMvc.perform(get(BeerController.BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateBeerByIdMethodNotAllowed() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post(BeerController.BASE_URL + "/" + id))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testUpdateBeerByIdNotFound() throws Exception {
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

        mockMvc.perform(put(BeerController.BASE_URL + "/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBeerByIdSuccess() throws Exception {
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

        mockMvc.perform(put(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beerId.toString()))
                .andExpect(jsonPath("$.beerName").value("Updated Beer"));
    }

    @Test
    void testUpdateBeerValidationFails() throws Exception {
        UUID beerId = UUID.randomUUID();
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", ""); // Should fail @NotBlank
        beerMap.put("beerStyle", "IPA");
        beerMap.put("upc", "123456");
        beerMap.put("price", 10.50);

        mockMvc.perform(put(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.beerName").exists());
    }

    @Test
    void testPatchBeerByIdSuccess() throws Exception {
        UUID beerId = UUID.randomUUID();
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "Patched Name");

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

        mockMvc.perform(patch(BeerController.BASE_URL + "/" + beerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beerId.toString()))
                .andExpect(jsonPath("$.beerName").value("Patched Name"));
    }

    @Test
    void testDeleteBeerByIdSuccess() throws Exception {
        UUID beerId = UUID.randomUUID();

        willDoNothing().given(beerService).deleteBeerById(beerId);

        mockMvc.perform(delete(BeerController.BASE_URL + "/" + beerId))
                .andExpect(status().isNoContent());

        then(beerService).should().deleteBeerById(beerId);
    }

    @Test
    void testDeleteBeerByIdNotFound() throws Exception {
        UUID beerId = UUID.randomUUID();

        willThrow(new NotFoundException("Beer", "id", beerId.toString()))
                .given(beerService).deleteBeerById(beerId);

        mockMvc.perform(delete(BeerController.BASE_URL + "/" + beerId))
                .andExpect(status().isNotFound());
    }
}
