package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * BasketResponse — DTO representing the shopping basket returned by the basket API.
 *
 * Used for both reading (GET /api/baskets/{id}) and writing (POST /api/baskets).
 * The Angular frontend sends this exact shape when saving basket changes, and
 * receives it back after every save.
 *
 * Note: The basket ID is a String (CUID/UUID) generated client-side by Angular
 * using the @paralleldrive/cuid2 library. This means the server never generates
 * basket IDs — Angular owns basket identity, which enables offline-first basket creation.
 *
 * Lombok annotations:
 *   @Data            : getters, setters, equals, hashCode, toString
 *   @Builder         : BasketResponse.builder().id("xyz").items(list).build()
 *   @AllArgsConstructor / @NoArgsConstructor : required for @Builder and Jackson
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BasketResponse {
    private String id;                    // Client-generated CUID (e.g. "clh12xyz...") — Redis key suffix
    private List<BasketItemResponse> items; // All items currently in the basket
}
