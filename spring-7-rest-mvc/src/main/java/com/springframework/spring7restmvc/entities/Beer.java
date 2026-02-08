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
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Beer entity representing a beer product in the catalog.
 * <p>
 * Uses JPA Auditing for automatic timestamp management.
 * Enforces unique beer names at the database level.
 */
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "beer", uniqueConstraints = {
        @UniqueConstraint(name = "uk_beer_name", columnNames = "beer_name")
})
public class Beer {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "beerId", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
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

    @Positive
    @NotNull
    private BigDecimal price;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
