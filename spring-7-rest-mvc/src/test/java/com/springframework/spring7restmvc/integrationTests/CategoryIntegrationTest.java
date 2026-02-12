package com.springframework.spring7restmvc.integrationTests;

import com.springframework.spring7restmvc.controllers.CategoryController;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Category;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Category entity.
 * Test the full stack: Controller -> Service -> Repository -> Database
 * Uses TestContainers for an isolated MySQL database per test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class CategoryIntegrationTest {
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
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    // ==================== Test Fixtures ====================

    private Category createCategory(String description) {
        return Category.builder()
                .description(description)
                .build();
    }

    private String createCategoryJson(String description) {
        return """
                {"description": "%s"}""".formatted(description);
    }

    private String escapeJson(String json) {
        return json.replace("\"", "\\\"");
    }

    private String createCategoryJsonWithBeers(String description, Beer beer) {
        return """
                {"description": "%s", "beers": [%s]}""".formatted(description, beer);
    }

    private Beer createBeer(String name, BeerStyle style, String upc, BigDecimal price) {
        return Beer.builder()
                .beerName(name)
                .beerStyle(style)
                .upc(upc)
                .price(price)
                .build();
    }

    // ==================== Create Category Tests ====================

    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        void should_createAndRetrieveCategory() throws Exception {
            // Given
            String categoryJson = createCategoryJson("Lager");

            // When - create
            MvcResult createResult = mockMvc.perform(post(CategoryController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(categoryJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andReturn();

            String location = createResult.getResponse().getHeader("Location");
            assertThat(location).isNotNull();

            mockMvc.perform(get(location))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Lager"));
        }

        @Test
        void should_ValidateRequiredFields() throws Exception {
            // Given - missing required field
            String categoryJson = createCategoryJson("");

            // When / Then
            mockMvc.perform(post(CategoryController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(categoryJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.description").exists())
                    .andExpect(jsonPath("$.title").value("Validation failed"));
        }

        @Test
        void should_PreventDuplicateBeerName() throws Exception {
            // Give - existing category
            Category category = categoryRepository.save(createCategory("Lager"));

            String duplicateJson = createCategoryJson("Lager");

            // When/Then
            mockMvc.perform(post(CategoryController.BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource already exists"));

        }
    }

    // ==================== Update Category Tests ====================
    @Nested
    @DisplayName("Update Category Tests")
    class UpdateCategoryTests {

        @Test
        void should_AllowUpdateWithSameName() throws Exception {
            // Given - existing category
            Category category = categoryRepository.save(createCategory("Lager"));

            // When - update with same name
            String updatedJson = createCategoryJson("Lager");

            // Then - should succeed
            mockMvc.perform(put(CategoryController.BASE_URL + "/" + category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatedJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Lager"));
        }

        @Test
        void should_PreventUpdateWithDuplicateName() throws Exception {
            // Given - existing category
            Category category = categoryRepository.save(createCategory("Lager"));
            categoryRepository.save(createCategory("Pilsner"));

            // When - update with duplicate name
            String duplicateJson = createCategoryJson("Pilsner");

            // Then - should fail
            mockMvc.perform(put(CategoryController.BASE_URL + "/" + category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource already exists"));
        }

        @Test
        void should_UpdateTimestampsOnUpdate() throws Exception {
            // Given - existing category
            Category category = categoryRepository.saveAndFlush(createCategory("Lager"));

            Category persisted = categoryRepository.findById(category.getId()).orElseThrow();

            LocalDateTime originalCreatedAt = persisted.getCreatedAt();
            LocalDateTime originalUpdatedAt = persisted.getUpdatedAt();

            String updatedJson = createCategoryJson("Pilsner");

            // WHen
            mockMvc.perform(put(CategoryController.BASE_URL + "/" + category.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatedJson))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            // Then
            Category updated = categoryRepository.findById(category.getId()).orElseThrow();

            assertThat(updated.getCreatedAt())
                    .isNotNull()
                    .isEqualTo(originalCreatedAt);

            assertThat(updated.getDescription()).isEqualTo("Pilsner");
        }

        @Test
        void should_Return404ForNonExistentCategory() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();
            String updateJson = createCategoryJson("Pilsner");

            // When/Then
            mockMvc.perform(put(CategoryController.BASE_URL + "/" + randomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource not found"));
        }
    }

    // ==================== Delete Beer Tests ====================

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        void should_DeleteCategory() throws Exception {
            // Given - existing category
            Category category = categoryRepository.save(createCategory("Lager"));

            // When - delete
            mockMvc.perform(delete(CategoryController.BASE_URL + "/" + category.getId()))
                    .andExpect(status().isNoContent());

            // Then - should not exist
            assertThat(categoryRepository.findById(category.getId())).isEmpty();
        }

        @Test
        void should_Return404ForNonExistentCategory() throws Exception {
            // Given
            UUID randomId = UUID.randomUUID();

            // When/Then
            mockMvc.perform(delete(CategoryController.BASE_URL + "/" + randomId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Query Category Tests ====================

    @Nested
    @DisplayName("Query Category Tests")
    class QueryCategoryTests {

        @Test
        void should_GetAllCategories() throws Exception {
            // Given - multiple categories
            categoryRepository.save(createCategory("Lager"));
            categoryRepository.save(createCategory("Pilsner"));
            categoryRepository.save(createCategory("Weissbier"));

            // When/Then
            mockMvc.perform(get(CategoryController.BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[*].description",
                            containsInAnyOrder("Lager", "Pilsner", "Weissbier")));
        }

        @Test
        void should_FilterCategoriesByName() throws Exception {
            // Given - multiple categories
            categoryRepository.save(createCategory("Lager"));
            categoryRepository.save(createCategory("Pilsner"));
            categoryRepository.save(createCategory("Weissbier"));

            // When/Then
            mockMvc.perform(get(CategoryController.BASE_URL + "?description=Lager"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].description", is("Lager")));
        }
    }

}
