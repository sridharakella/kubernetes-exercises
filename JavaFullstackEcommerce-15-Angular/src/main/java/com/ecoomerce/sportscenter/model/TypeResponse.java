package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TypeResponse — lightweight DTO for a product type/category.
 *
 * Returned by GET /api/products/types. Mirrors BrandResponse in structure.
 * The Angular store uses this list to populate the type/category filter sidebar
 * (e.g. "Shoes", "Rackets", "Gloves", "Footballs").
 *
 * Only id and name are exposed — the List<Product> back-reference on the Type entity
 * is never included in the response.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TypeResponse {
    private Integer id;   // Type primary key — used as the filter parameter (?typeId=2)
    private String name;  // Type/category display name (e.g. "Shoes", "Rackets")
}
