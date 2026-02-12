package com.springframework.spring7restmvc.integrationTests;

import tools.jackson.databind.ObjectMapper;
import com.springframework.spring7restmvc.controllers.BeerController;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Category;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Beer API endpoints.
 * Tests the full stack: Controller -> Service -> Repository -> Database
 * Uses TestContainers for an isolated MySQL database per test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class BeerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    // ==================== Test Fixtures ====================

    private Beer createBeer(String name, BeerStyle style, String upc, BigDecimal price) {
        return Beer.builder()
                .beerName(name)
                .beerStyle(style)
                .upc(upc)
                .price(price)
                .build();
    }

    private Beer createBeer(String beerMame, BeerStyle style, String upc, Integer quantity, BigDecimal price) {
        return Beer.builder()
                .beerName(beerMame)
                .beerStyle(style)
                .upc(upc)
                .quantityOnHand(quantity)
                .price(price)
                .build();
    }

    private String createBeerJson(String name, String style, String upc, Integer quantity, BigDecimal price) {
        String priceJson = (price == null) ? "0.00" : price.toPlainString();

        return """
            {
              "beerName": "%s",
              "beerStyle": "%s",
              "upc": "%s",
              "quantityOnHand": %d,
              "price": %s
            }
            """.formatted(
                escapeJson(name),
                escapeJson(style),
                escapeJson(upc),
                quantity != null ? quantity : 0,
                priceJson
        );
    }

    private String createBeerJsonWithCategories(String name, String style, String upc, Integer quantity,
                                                BigDecimal price, Set<UUID> categoryIds) {

        String priceJson = (price == null) ? "0.00" : price.toPlainString();

        String categoryIdsJson = (categoryIds == null) ? "" : categoryIds.stream()
                .map(UUID::toString)
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(", "));

        return """
            {
              "beerName": "%s",
              "beerStyle": "%s",
              "upc": "%s",
              "quantityOnHand": %d,
              "price": %s,
              "categoryIds": [%s]
            }
            """.formatted(
                escapeJson(name),
                escapeJson(style),
                escapeJson(upc),
                quantity != null ? quantity : 0,
                priceJson,
                categoryIdsJson
        );
    }

    private Category createCategory(String description) {
        return Category.builder()
                .description(description)
                .build();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ==================== Create Beer Tests ====================
    @Nested
    @DisplayName("Create Beer Tests")
    class CreateBeerTests {

        @Test
        void should_CreateAndRetrieveBeer() throws Exception {
            // Given
            String beerJson = createBeerJson("Pilsner Urquell", "LAGER", "123456", 100, new BigDecimal("2.50"));

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
        void should_ValidateRequiredFields() throws Exception {
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
        void should_ValidatePositivePrice() throws Exception {
            // Given - negative price
            String invalidJson = createBeerJson("Test Beer", "LAGER", "123456", 100, new BigDecimal("-1.50"));

            // When/Then
            mockMvc.perform(post(BeerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.price").exists());
        }

        @Test
        void should_HandleMalformedJson() throws Exception {
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
        void should_PreventDuplicateBeerName() throws Exception {
            // Given - existing beer
            beerRepository.save(createBeer("Duplicate Beer", BeerStyle.LAGER, "111", new BigDecimal("2.00")));

            String duplicateJson = createBeerJson("Duplicate Beer", "IPA", "222", 50, new BigDecimal("3.00"));

            // When/Then
            mockMvc.perform(post(BeerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource already exists"));
        }
    }

    // ==================== Update Beer Tests ====================

    @Nested
    @DisplayName("Update Beer Tests")
    class UpdateBeerTests {

        @Test
        void should_AllowUpdateWithSameName() throws Exception {
            // Given - existing beer
            Beer beer = beerRepository.save(createBeer("Staropramen", BeerStyle.LAGER, "123456", new BigDecimal("2.50")));

            // When - update with same name but different price
            String updateJson = createBeerJson("Staropramen", "LAGER", "123456", 200, new BigDecimal("3.00"));

            // Then - should_ succeed
            mockMvc.perform(put(BeerController.BASE_URL + "/" + beer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.beerName").value("Staropramen"))
                    .andExpect(jsonPath("$.price").value(3.00))
                    .andExpect(jsonPath("$.quantityOnHand").value(200));

            // Verify in database
            Beer updated = beerRepository.findById(beer.getId()).orElseThrow();
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(updated.getQuantityOnHand()).isEqualTo(200);
        }

        @Test
        void should_PreventUpdateWithDuplicateName() throws Exception {
            // Given - two existing beers
            Beer beer1 = beerRepository.save(createBeer("Zlaty Bazant", BeerStyle.LAGER, "111", new BigDecimal("1.50")));
            beerRepository.save(createBeer("Kozel", BeerStyle.LAGER, "222", new BigDecimal("1.80")));

            // When - try to rename beer1 to beer2's name
            String updateJson = createBeerJson("Kozel", "LAGER", "111", 100, new BigDecimal("1.50"));

            // Then - should_ fail
            mockMvc.perform(put(BeerController.BASE_URL + "/" + beer1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource already exists"));
        }

        @Test
        void should_UpdateTimestampsOnUpdate() throws Exception {
            // Given
            Beer beer = beerRepository.saveAndFlush(
                    createBeer("Time Test Beer", BeerStyle.LAGER, "TIME123", new BigDecimal("2.00"))
            );

            Beer persisted = beerRepository.findById(beer.getId()).orElseThrow();

            LocalDateTime originalCreatedAt = persisted.getCreatedAt();
            LocalDateTime originalUpdatedAt = persisted.getUpdatedAt();

            String updateJson = createBeerJson(
                    "Time Test Beer", "IPA", "TIME123", 100, new BigDecimal("2.50")
            );

            // When
            mockMvc.perform(put(BeerController.BASE_URL + "/" + beer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            // Then
            Beer updated = beerRepository.findById(beer.getId()).orElseThrow();

            assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);

            // miesto isAfter:
            assertThat(updated.getUpdatedAt())
                    .isNotNull()
                    .isNotEqualTo(originalUpdatedAt);

            assertThat(updated.getBeerStyle()).isEqualTo(BeerStyle.IPA);
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("2.50"));
            assertThat(updated.getQuantityOnHand()).isEqualTo(100);
        }

        @Test
        void should_Return404ForNonExistentBeer() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();

            String updateJson = createBeerJson("Test", "LAGER", "123", 100, new BigDecimal("2.50"));

            // When/Then
            mockMvc.perform(put(BeerController.BASE_URL + "/" + randomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource not found"));
        }
    }

    // ==================== Patch Beer Tests ====================

    @Nested
    @DisplayName("Patch Beer Tests")
    class PatchBeerTests {

        @Test
        void should_PatchPartialFields() throws Exception {
            // Given - existing beer
            Beer beer = beerRepository.save(createBeer("Corgoň", BeerStyle.LAGER, "333333", 100, new BigDecimal("1.90")));

            // When - patch only price and quantity
            String patchJson = """
                {
                  "quantityOnHand": 150,
                  "price": 2.20
                }
                """;

            // Then - should_ update only those fields
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
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("2.20"));
        }

        @Test
        void should_AllowPatchWithSameName() throws Exception {
            // Given
            Beer beer = beerRepository.save(createBeer("Šariš", BeerStyle.LAGER, "444444", new BigDecimal("1.70")));

            // When - patch with same name but different price
            String patchJson = """
                {
                  "beerName": "Šariš",
                  "price": 2.00
                }
                """;

            // Then - should_ succeed
            mockMvc.perform(patch(BeerController.BASE_URL + "/" + beer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(2.00));
        }

        @Test
        void should_Return404ForNonExistentBeer() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();
            String patchJson = """
                {
                  "price": 5.00
                }
                """;

            // When/Then
            mockMvc.perform(patch(BeerController.BASE_URL + "/" + randomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Delete Beer Tests ====================

    @Nested
    @DisplayName("Delete Beer Tests")
    class DeleteBeerTests {

        @Test
        void should_DeleteBeer() throws Exception {
            // Given
            Beer beer = beerRepository.save(createBeer("Steiger", BeerStyle.LAGER, "555555", new BigDecimal("1.40")));

            // When
            mockMvc.perform(delete(BeerController.BASE_URL + "/" + beer.getId()))
                    .andExpect(status().isNoContent());

            // Then - verify deletion
            assertThat(beerRepository.findById(beer.getId())).isEmpty();
        }

        @Test
        void should_Return404ForNonExistentBeer() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();

            // When/Then
            mockMvc.perform(delete(BeerController.BASE_URL + "/" + randomId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Query Beer Tests ====================

    @Nested
    @DisplayName("Query Beer Tests")
    class QueryBeerTests {

        @Test
        void should_GetAllBeers() throws Exception {
            // Given - multiple beers
            beerRepository.save(createBeer("Beer 1", BeerStyle.LAGER, "111", new BigDecimal("1.50")));
            beerRepository.save(createBeer("Beer 2", BeerStyle.IPA, "222", new BigDecimal("2.50")));
            beerRepository.save(createBeer("Beer 3", BeerStyle.STOUT, "333", new BigDecimal("3.50")));

            // When/Then
            mockMvc.perform(get(BeerController.BASE_URL)
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[*].beerName",
                            containsInAnyOrder("Beer 1", "Beer 2", "Beer 3")));
        }

        @Test
        void should_FilterBeersByName() throws Exception {
            // Given
            beerRepository.save(createBeer("Galaxy IPA", BeerStyle.IPA, "g1", new BigDecimal("2.50")));
            beerRepository.save(createBeer("Super Galaxy Lager", BeerStyle.LAGER, "g2", new BigDecimal("1.90")));
            beerRepository.save(createBeer("Nebula Stout", BeerStyle.STOUT, "n1", new BigDecimal("3.10")));

            // When/Then
            mockMvc.perform(get(BeerController.BASE_URL)
                            .param("beerName", "galaxy")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].beerName", containsInAnyOrder("Galaxy IPA", "Super Galaxy Lager")))
                    .andExpect(jsonPath("$.content[*].beerName", not(hasItem("Nebula Stout"))));
        }

        @Test
        void should_FilterBeersByStyle() throws Exception {
            // Given
            beerRepository.save(createBeer("Beer 1", BeerStyle.LAGER, "1", new BigDecimal("1.50")));
            beerRepository.save(createBeer("Beer 2", BeerStyle.IPA, "2", new BigDecimal("2.50")));
            beerRepository.save(createBeer("Beer 3", BeerStyle.LAGER, "3", new BigDecimal("1.80")));

            // When/Then
            mockMvc.perform(get(BeerController.BASE_URL)
                            .param("beerStyle", "LAGER")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].beerStyle", everyItem(is("LAGER"))));
        }

        @Test
        void should_Return404ForNonExistentBeer() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();

            // When/Then
            mockMvc.perform(get(BeerController.BASE_URL + "/" + randomId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource not found"))
                    .andExpect(jsonPath("$.detail").value(containsString("Beer not found")));
        }

        @Test
        void should_ReturnEmptyPageWhenNoBeersExist() throws Exception {
            // When/Then
            mockMvc.perform(get(BeerController.BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page.totalElements").value(0))
                    .andExpect(jsonPath("$.page.totalPages").value(0))
                    .andExpect(jsonPath("$.page.number").value(0));
        }
    }

    // ==================== Beer Category Tests ====================

    @Nested
    @DisplayName("Beer Category Integration Tests")
    class BeerCategoryTests {

        @Test
        void should_CreateBeerWithCategories() throws Exception {
            // Given - create categories first
            Category lager = categoryRepository.save(createCategory("Lager"));
            Category pilsner = categoryRepository.save(createCategory("Pilsner"));

            String beerJson = createBeerJsonWithCategories(
                    "Pilsner Urquell",
                    BeerStyle.LAGER.toString(),
                    "123456",
                    100,
                    BigDecimal.valueOf(2.5),
                    Set.of(lager.getId(), pilsner.getId())
            );

            // When
            MvcResult result = mockMvc.perform(post(BeerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(beerJson))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            String location = result.getResponse().getHeader("Location");
            mockMvc.perform(get(location))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories", hasSize(2)))
                    .andExpect(jsonPath("$.categories[*].description",
                            containsInAnyOrder("Lager", "Pilsner")));
        }

        @Test
        void should_RejectInvalidCategoryId() throws Exception {
            // Given - non-existent category
            UUID invalidCategoryId = UUID.randomUUID();

            String beerJson = createBeerJsonWithCategories(
                    "Test Beer", "IPA", "123", 100,
                    new BigDecimal("10.00"), Set.of(invalidCategoryId)
            );

            // When/Then
            mockMvc.perform(post(BeerController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(beerJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource not found"))
                    .andExpect(jsonPath("$.detail").value(containsString("Category not found")));
        }

        @Test
        void should_SetCategoriesForExistingBeer() throws Exception {
            // Given
            Beer beer = beerRepository.save(createBeer("Test Beer", BeerStyle.IPA, "123", new BigDecimal("10.00")));
            Category ipa = categoryRepository.save(createCategory("IPA"));
            Category hoppy = categoryRepository.save(createCategory("Hoppy"));

            Set<UUID> categoryIds = Set.of(ipa.getId(), hoppy.getId());

            // When/Then
            mockMvc.perform(put("/api/v1/beers/{beerId}/categories", beer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryIds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories", hasSize(2)))
                    .andExpect(jsonPath("$.categories[*].description",
                            containsInAnyOrder("IPA", "Hoppy")));

            // Verify in database
            entityManager.flush();
            entityManager.clear();

            Beer updated = beerRepository.findById(beer.getId()).orElseThrow();
            assertThat(updated.getCategories()).hasSize(2);
        }

        @Test
        void should_RemoveAllCategoriesWithEmptySet() throws Exception {
            // Given - beer with categories
            Beer beer = beerRepository.save(createBeer("Test Beer", BeerStyle.IPA, "123", new BigDecimal("10.00")));
            Category category = categoryRepository.save(createCategory("IPA"));
            beer.addCategory(category);
            beerRepository.save(beer);

            Set<UUID> emptySet = Set.of();

            // When
            mockMvc.perform(put("/api/v1/beers/{beerId}/categories", beer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptySet)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories").isEmpty());

            // Then - verify in database
            entityManager.flush();
            entityManager.clear();

            Beer updated = beerRepository.findById(beer.getId()).orElseThrow();
            assertThat(updated.getCategories()).isEmpty();
        }

        @Test
        void should_AddSingleCategoryToBeer() throws Exception {
            // Given
            Beer beer = beerRepository.save(createBeer("Test Beer", BeerStyle.IPA, "123", new BigDecimal("10.00")));
            Category category = categoryRepository.save(createCategory("IPA"));

            // When/Then
            mockMvc.perform(post("/api/v1/beers/{beerId}/categories/{categoryId}",
                            beer.getId(), category.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories", hasSize(1)))
                    .andExpect(jsonPath("$.categories[0].id").value(category.getId().toString()))
                    .andExpect(jsonPath("$.categories[0].description").value("IPA"));
        }

        @Test
        void should_RemoveSingleCategoryFromBeer() throws Exception {
            // Given - beer with category
            Beer beer = beerRepository.save(createBeer("Test Beer", BeerStyle.IPA, "123", new BigDecimal("10.00")));
            Category category = categoryRepository.save(createCategory("IPA"));
            beer.addCategory(category);
            beerRepository.save(beer);

            // When
            mockMvc.perform(delete("/api/v1/beers/{beerId}/categories/{categoryId}",
                            beer.getId(), category.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories").isEmpty());

            // Then - verify in database
            entityManager.flush();
            entityManager.clear();

            Beer updated = beerRepository.findById(beer.getId()).orElseThrow();
            assertThat(updated.getCategories()).isEmpty();
        }

        @Test
        void should_MaintainBidirectionalRelationship() throws Exception {
            // Given
            Beer beer = beerRepository.save(createBeer("Test Beer", BeerStyle.IPA, "123", new BigDecimal("10.00")));
            Category category = categoryRepository.save(createCategory("IPA"));

            // When - add category to beer
            mockMvc.perform(post("/api/v1/beers/{beerId}/categories/{categoryId}",
                            beer.getId(), category.getId()))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            // Then - verify bidirectional relationship
            Beer updatedBeer = beerRepository.findById(beer.getId()).orElseThrow();
            Category updatedCategory = categoryRepository.findById(category.getId()).orElseThrow();

            assertThat(updatedBeer.getCategories()).hasSize(1);
            assertThat(updatedCategory.getBeers()).hasSize(1);
            assertThat(updatedCategory.getBeers()).contains(updatedBeer);
        }
    }
}