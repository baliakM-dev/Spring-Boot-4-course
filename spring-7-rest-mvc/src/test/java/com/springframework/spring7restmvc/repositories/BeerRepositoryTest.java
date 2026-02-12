package com.springframework.spring7restmvc.repositories;

import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Category;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BeerRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        // odporúčané pri TC:
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired BeerRepository beerRepository;
    @Autowired CategoryRepository categoryRepository;

    @BeforeEach
    void clean() {
        beerRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private Beer beer(String name) {
        return Beer.builder()
                .beerName(name)
                .beerStyle(BeerStyle.LAGER)
                .upc(UUID.randomUUID().toString())
                .price(new BigDecimal("2.50"))
                .quantityOnHand(100)
                .build();
    }

    private Category category(String desc) {
        return Category.builder()
                .description(desc)
                .build();
    }

    @Test
    void findByIdWithCategories_shouldFetchCategories() {
        // Given
        Category lager = categoryRepository.save(category("Lager"));
        Category pilsner = categoryRepository.save(category("Pilsner"));

        Beer saved = beerRepository.save(beer("Pilsner Urquell"));
        saved.addCategory(lager);
        saved.addCategory(pilsner);
        beerRepository.saveAndFlush(saved);

        // When
        Optional<Beer> loadedOpt = beerRepository.findByIdWithCategories(saved.getId());

        // Then
        assertThat(loadedOpt).isPresent();
        Beer loaded = loadedOpt.get();

        assertThat(loaded.getCategories())
                .extracting(Category::getDescription)
                .containsExactlyInAnyOrder("Lager", "Pilsner");
    }

    @Test
    void existsByBeerNameIgnoreCase_shouldWork() {
        // Given
        beerRepository.saveAndFlush(beer("Staropramen"));

        // Then
        assertThat(beerRepository.existsByBeerNameIgnoreCase("staropramen")).isTrue();
        assertThat(beerRepository.existsByBeerNameIgnoreCase("STAROPRAMEN")).isTrue();
        assertThat(beerRepository.existsByBeerNameIgnoreCase("Kozel")).isFalse();
    }

    @Test
    void existsByBeerNameIgnoreCaseAndIdNot_shouldExcludeCurrentBeer() {
        // Given
        Beer b1 = beerRepository.saveAndFlush(beer("Kozel"));
        Beer b2 = beerRepository.saveAndFlush(beer("Staropramen"));

        // Then: kontrola “či existuje Kozel okrem b1” -> false
        assertThat(beerRepository.existsByBeerNameIgnoreCaseAndIdNot("kozel", b1.getId())).isFalse();

        // A kontrola “či existuje Staropramen okrem b1” -> true (lebo je to b2)
        assertThat(beerRepository.existsByBeerNameIgnoreCaseAndIdNot("staropramen", b1.getId())).isTrue();
    }

    @Test
    void entityGraphQueries_shouldReturnBeersAndNotLoseResults() {
        // Given
        Category c1 = categoryRepository.save(category("A"));
        Category c2 = categoryRepository.save(category("B"));

        Beer b1 = beerRepository.save(beer("Alpha"));
        b1.addCategory(c1);

        Beer b2 = beerRepository.save(beer("Beta"));
        b2.addCategory(c2);

        beerRepository.saveAllAndFlush(Set.of(b1, b2));

        // When (EntityGraph metóda)
        Page<Beer> page = beerRepository.findAllByBeerNameContainingIgnoreCase("a", PageRequest.of(0, 10));

        // Then (Alpha aj Beta obsahujú "a" v mene? "Beta" áno, "Alpha" áno)
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(Beer::getBeerName)
                .containsExactlyInAnyOrder("Alpha", "Beta");

        // a categories sú dostupné
        assertThat(page.getContent().get(0).getCategories()).isNotNull();
    }
}