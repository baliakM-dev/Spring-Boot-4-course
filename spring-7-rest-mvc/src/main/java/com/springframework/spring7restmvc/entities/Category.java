package com.springframework.spring7restmvc.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Category entity for beer classification.
 * <p>
 * Uses JPA Auditing for automatic timestamp management.
 * Inverse side of Beer many-to-many relationship.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"description"}, name = "category_description_unique")
        })
public class Category {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "category_id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Version
    private Integer version;

    @NotBlank
    @Size(min = 1, max = 50)
    private String description;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Inverse side of many-to-many relationship.
     * Uses mappedBy to indicate Beer is the owning side.
     * Builder.Default ensures initialization even with builder pattern.
     */
    @Builder.Default
    @ManyToMany(mappedBy = "categories")
    private Set<Beer> beers = new HashSet<>();

    /**
     * Helper method to add a beer to this category.
     * Maintains bidirectional consistency by updating both sides.
     * Prevents infinite recursion by checking if beer is already added.
     *
     * @param beer the beer to add
     */
    public void addBeer(Beer beer) {
        if (beer != null && !this.beers.contains(beer)) {
            this.beers.add(beer);
            beer.getCategories().add(this);
        }
    }

    /**
     * Helper method to remove a beer from this category.
     * Maintains bidirectional consistency.
     *
     * @param beer the beer to remove
     */
    public void removeBeer(Beer beer) {
        if (beer != null) {
            this.beers.remove(beer);
            beer.getCategories().remove(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return id != null && id.equals(category.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
