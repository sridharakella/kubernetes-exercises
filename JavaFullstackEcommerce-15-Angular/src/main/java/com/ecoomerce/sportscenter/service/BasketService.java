package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.entity.Basket;
import com.ecoomerce.sportscenter.model.BasketResponse;

import java.util.List;

/**
 * BasketService — service interface defining basket operations backed by Redis.
 *
 * The basket is the shopping cart. All basket data lives in Redis (not MySQL)
 * because baskets are:
 *   - Temporary: deleted after checkout or when the user clears their cart
 *   - Write-heavy: updated on every add/remove/quantity change
 *   - Fast to access: no complex queries or JOINs needed
 *
 * The concrete implementation is BasketServiceImpl.
 */
public interface BasketService {

    /**
     * Retrieve all baskets from Redis.
     * Primarily for debugging; not called by the Angular frontend in normal use.
     *
     * @return list of all BasketResponse DTOs currently stored in Redis
     */
    List<BasketResponse> getAllBaskets();

    /**
     * Retrieve a single basket by its client-generated ID.
     * Returns null if no basket with the given ID exists (Angular handles the null case
     * by creating a new empty basket).
     *
     * @param basketId the CUID/UUID string identifying the basket (Redis key suffix)
     * @return the BasketResponse DTO, or null if not found
     */
    BasketResponse getBasketById(String basketId);

    /**
     * Delete a basket from Redis by ID.
     * Called after a successful checkout to free the Redis memory.
     *
     * @param basketId the basket ID to delete
     */
    void deleteBasketById(String basketId);

    /**
     * Create a new basket or update an existing one (upsert).
     * Redis save() always upserts — if a basket with the given ID exists, it is overwritten.
     * Angular sends the full basket state on every change, so this is always a full replace.
     *
     * @param basket the Basket entity to persist in Redis
     * @return the saved BasketResponse DTO (re-read from Redis after save)
     */
    BasketResponse createBasket(Basket basket);
}
