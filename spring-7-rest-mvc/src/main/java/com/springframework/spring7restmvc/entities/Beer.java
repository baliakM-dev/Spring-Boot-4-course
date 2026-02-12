package com.springframework.spring7restmvc.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Beer entity representing a beer product in the catalog.
 * <p>
 * Uses JPA Auditing for automatic timestamp management.
 * Enforces unique beer names at the database level.
 * Owning side of the many-to-many relationship with Category.
 */
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Beer {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "beer_id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Version
    private Integer version;

    @NotBlank
    @Size(min = 1, max = 50)
    private String beerName;

    @NonNull
    private BeerStyle beerStyle;

    @NotBlank
    @Size(min = 1, max = 50)
    private String upc;

    private Integer quantityOnHand;

    /**
     * Owning side of a many-to-many relationship with Category.
     * JoinTable defines the association table structure.
     * Builder.Default ensures Set is initialized even when using builder.
     */
    @Builder.Default
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "beer_category",
            joinColumns = @JoinColumn(name = "beer_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    /**
     * Helper method to add a category to this beer.
     * Maintains bidirectional consistency by updating both sides.
     * Prevents infinite recursion by checking if a category is already added.
     *
     * @param category the category to add
     */
    public void addCategory(Category category) {
        if (category != null && !this.categories.contains(category)) {
            this.categories.add(category);
            category.getBeers().add(this);
        }
    }

    /**
     * Helper method to remove a category from this beer.
     * Maintains bidirectional consistency.
     *
     * @param category the category to remove
     */
    public void removeCategory(Category category) {
        if (category != null) {
            this.categories.remove(category);
            category.getBeers().remove(this);
        }
    }

    @Positive
    @NotNull
    private BigDecimal price;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Beer beer)) return false;
        return id != null && id.equals(beer.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
