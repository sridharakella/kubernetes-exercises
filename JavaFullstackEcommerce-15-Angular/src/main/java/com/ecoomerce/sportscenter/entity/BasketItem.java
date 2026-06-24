package com.ecoomerce.sportscenter.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * BasketItem — Redis entity representing a single product line item in a shopping basket.
 *
 * Stored as part of the Basket hash in Redis (nested within the Basket entity).
 * Each BasketItem is a denormalised snapshot of the product at the time it was added —
 * this avoids a join to the product table every time the basket is read.
 *
 * Denormalisation rationale:
 *   - The basket stores name, price, brand, and type directly (not a foreign key to Product).
 *   - This means the basket still reflects the price/name the user saw even if the
 *     product catalog is updated later — important for pricing integrity.
 *
 * @RedisHash("BasketItem") : Redis storage key prefix for standalone BasketItem hashes
 * @Data                    : Lombok — auto-generates getters, setters, equals, hashCode, toString
 */
@Data
@RedisHash("BasketItem")    // Redis key prefix (used if BasketItem is stored independently)
public class BasketItem {

    @Id                        // Redis hash key field (product ID used as the item identifier)
    private Integer id;        // Product ID — uniquely identifies the product within this basket

    private String name;       // Product name at time of adding to basket (e.g. "Yonex Racket")
    private String description;// Product description at time of adding (denormalised from Product)
    private Long price;        // Unit price at time of adding (in smallest currency unit)
    private String pictureUrl; // Product image URL (denormalised from Product)
    private String productBrand; // Brand name as a string (denormalised — avoids Brand join)
    private String productType;  // Type name as a string (denormalised — avoids Type join)
    private Integer quantity;  // Number of units of this product in the basket
}
