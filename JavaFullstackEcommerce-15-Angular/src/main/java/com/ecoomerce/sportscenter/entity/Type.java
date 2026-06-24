package com.ecoomerce.sportscenter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Type — JPA entity representing a product category/type (e.g. Shoes, Rackets, Footballs).
 *
 * Maps to the "Type" table in the sportscenter MySQL schema.
 * One type can have many products (one-to-many relationship).
 * Mirrors the structure of Brand — both are lookup/classification tables.
 *
 * Lombok annotations:
 *   @Data          : auto-generates getters, setters, equals, hashCode, toString
 *   @AllArgsConstructor : all-fields constructor
 *   @NoArgsConstructor  : no-arg constructor (required by JPA spec)
 *   @Builder       : enables builder pattern for construction
 */
@Entity
@Table(name="Type")    // Maps to the "Type" table in MySQL
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Type {

    @Id                                                     // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // Auto-incremented by MySQL
    @Column(name="Id")                                     // Maps to the "Id" column
    private Integer id;

    @Column(name="Name")                                   // Maps to the "Name" column
    private String name;                                   // Type name shown in the UI (e.g. "Shoes", "Rackets")

    /**
     * One type can classify many products (one-to-many).
     *
     * mappedBy = "type" : the "type" field in the Product entity owns this relationship
     *                     (the foreign key ProductTypeId lives in the Product table)
     * fetch = FetchType.LAZY : products list is only loaded when explicitly accessed
     */
    @OneToMany(mappedBy = "type", fetch = FetchType.LAZY)
    private List<Product> products;
}
