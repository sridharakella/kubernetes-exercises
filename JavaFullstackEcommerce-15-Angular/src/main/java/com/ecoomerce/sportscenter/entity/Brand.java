package com.ecoomerce.sportscenter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Brand — JPA entity representing a sports brand (e.g. Nike, Adidas, Yonex).
 *
 * Maps to the "Brand" table in the sportscenter MySQL schema.
 * One brand can have many products (one-to-many relationship).
 *
 * Lombok annotations:
 *   @Data          : auto-generates getters, setters, equals, hashCode, toString
 *   @AllArgsConstructor : all-fields constructor
 *   @NoArgsConstructor  : no-arg constructor (required by JPA spec)
 *   @Builder       : enables builder pattern for clean construction
 */
@Entity
@Table(name="Brand")   // Maps to the "Brand" table in MySQL
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Brand {

    @Id                                                     // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // Auto-incremented by MySQL
    @Column(name="Id")                                     // Maps to the "Id" column
    private Integer id;

    @Column(name="Name")                                   // Maps to the "Name" column
    private String name;                                   // Brand name shown in the UI (e.g. "Nike", "Yonex")

    /**
     * One brand can supply many products (one-to-many).
     *
     * mappedBy = "brand" : the "brand" field in the Product entity owns this relationship
     *                      (the foreign key is in the Product table, not here)
     * fetch = FetchType.LAZY : the list of products is NOT loaded unless explicitly accessed
     *                          — avoids loading entire product catalog just to get a brand name
     */
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    private List<Product> products;
}
