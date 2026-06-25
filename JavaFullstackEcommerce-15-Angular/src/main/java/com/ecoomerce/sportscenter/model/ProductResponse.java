package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductResponse — DTO (Data Transfer Object) returned by the product API endpoints.
 *
 * This class is what the Angular frontend receives from GET /api/products and
 * GET /api/products/{id}. It deliberately does NOT expose the Product JPA entity
 * directly for two reasons:
 *   1. The Product entity has @ManyToOne Brand and Type fields with @OneToMany back-references.
 *      Exposing the entity would cause Jackson to serialise infinitely (product → brand → products → brand ...).
 *   2. It decouples the database schema from the API contract — the entity can change
 *      internally without breaking the API consumers.
 *
 * Key design choices:
 *   - productBrand and productType are plain Strings (not Brand/Type objects).
 *     The service layer resolves them by calling product.getBrand().getName().
 *   - price is Long (not BigDecimal) — stored in the smallest currency unit (e.g. pence/cents)
 *     to avoid floating-point precision issues.
 *
 * Lombok annotations:
 *   @Data            : generates getters, setters, equals, hashCode, toString
 *   @Builder         : enables ProductResponse.builder().id(1).name("...").build() pattern
 *   @AllArgsConstructor : all-fields constructor (needed by @Builder internally)
 *   @NoArgsConstructor  : no-args constructor (needed by Jackson for JSON deserialisation)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Integer id;          // Product primary key
    private String name;         // Product display name
    private String description;  // Full product description
    private Long price;          // Price in smallest currency unit (e.g. 4999 = £49.99)
    private String pictureUrl;   // URL to the product image
    private String productType;  // Type/category name resolved from Type entity (e.g. "Shoes")
    private String productBrand; // Brand name resolved from Brand entity (e.g. "Nike")
}
