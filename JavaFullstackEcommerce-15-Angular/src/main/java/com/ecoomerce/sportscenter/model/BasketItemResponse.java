package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BasketItemResponse — DTO representing a single item inside a basket.
 *
 * Mirrors the BasketItem Redis entity but is used as the HTTP payload shape.
 * All product details are stored directly in the basket item (denormalised) rather
 * than as foreign key references. This is intentional:
 *
 *   Price snapshot: The price is captured at the moment the item is added.
 *     If the product price changes later, the basket still shows the original price
 *     the user saw — ensuring pricing integrity through checkout.
 *
 *   Brand/Type snapshot: productBrand and productType are stored as strings, not IDs.
 *     The basket display doesn't need to look up brand/type names from MySQL,
 *     making basket reads fast and independent of the product catalog.
 *
 * Matches the Angular BasketItem TypeScript interface field-for-field.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BasketItemResponse {
    private Integer id;          // Product ID — used to identify the item (not the basket item)
    private String name;         // Product name snapshot at time of add
    private String description;  // Product description snapshot
    private Long price;          // Price snapshot in smallest currency unit at time of add
    private String pictureUrl;   // Product image URL snapshot
    private String productBrand; // Brand name snapshot (denormalised String, not FK)
    private String productType;  // Type/category name snapshot (denormalised String, not FK)
    private Integer quantity;    // Number of this item in the basket
}
