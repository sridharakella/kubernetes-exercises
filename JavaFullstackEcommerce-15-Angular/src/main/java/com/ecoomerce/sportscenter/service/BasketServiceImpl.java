package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.entity.Basket;
import com.ecoomerce.sportscenter.entity.BasketItem;
import com.ecoomerce.sportscenter.model.BasketItemResponse;
import com.ecoomerce.sportscenter.model.BasketResponse;
import com.ecoomerce.sportscenter.repository.BasketRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BasketServiceImpl — concrete implementation of BasketService backed by Redis.
 *
 * All operations delegate to BasketRepository, which is a Spring Data Redis
 * CrudRepository. Spring Data Redis auto-implements the repository interface and
 * handles serialisation/deserialisation of Basket objects to/from Redis hashes.
 *
 * Entity-to-DTO pattern:
 *   Basket      → BasketResponse      (read direction: Redis entity → HTTP response)
 *   BasketItem  → BasketItemResponse  (read direction: Redis entity → HTTP response)
 *
 * @Service : registers this as a Spring-managed singleton service bean
 * @Log4j2  : injects a Log4j2 logger via Lombok for structured logging
 */
@Service
@Log4j2
public class BasketServiceImpl implements BasketService {
    private final BasketRepository basketRepository; // Spring Data Redis CrudRepository

    /**
     * Constructor injection — Spring injects the BasketRepository dependency.
     *
     * @param basketRepository Redis-backed CRUD repository for Basket entities
     */
    public BasketServiceImpl(BasketRepository basketRepository) {
        this.basketRepository = basketRepository;
    }

    /**
     * {@inheritDoc}
     *
     * CrudRepository.findAll() returns Iterable<Basket>; casting to List is safe
     * because Spring Data Redis returns a List-compatible implementation.
     */
    @Override
    public List<BasketResponse> getAllBaskets() {
        log.info("Fetching all Baskets");
        // findAll() scans all "Basket:*" keys in Redis and deserialises each one
        List<Basket> basketList = (List<Basket>) basketRepository.findAll();
        // Convert each Basket entity to a BasketResponse DTO
        List<BasketResponse> basketResponses = basketList.stream()
                .map(this::convertToBasketResponse)
                .collect(Collectors.toList());
        log.info("Fetched all Baskets");
        return basketResponses;
    }

    /**
     * {@inheritDoc}
     *
     * Returns null instead of throwing an exception when the basket is not found,
     * because a missing basket is a normal case (first visit, cleared cart).
     * Angular handles null by creating a fresh empty basket client-side.
     */
    @Override
    public BasketResponse getBasketById(String basketId) {
        log.info("Fetching Basket by Id: {}", basketId);
        // findById returns Optional<Basket>; empty means key "Basket:<basketId>" doesn't exist in Redis
        Optional<Basket> basketOptional = basketRepository.findById(basketId);
        if (basketOptional.isPresent()) {
            Basket basket = basketOptional.get();
            log.info("Fetched Basket by Id: {}", basketId);
            return convertToBasketResponse(basket);
        } else {
            log.info("Basket not found by Id: {}", basketId);
            return null; // Angular creates a new empty basket when null is returned
        }
    }

    /**
     * {@inheritDoc}
     *
     * deleteById removes the "Basket:<basketId>" key from Redis.
     * No-op if the key doesn't exist (Redis DEL on a missing key is safe).
     */
    @Override
    public void deleteBasketById(String basketId) {
        log.info("Deleting Basket by Id: {}", basketId);
        basketRepository.deleteById(basketId);
        log.info("Deleted Basket by Id: {}", basketId);
    }

    /**
     * {@inheritDoc}
     *
     * basketRepository.save() is an upsert in Redis:
     *   - If "Basket:<id>" doesn't exist → creates a new Redis hash
     *   - If "Basket:<id>" exists → overwrites it with the new data
     * This is why the same endpoint handles both create and update.
     */
    @Override
    public BasketResponse createBasket(Basket basket) {
        log.info("Creating Basket");
        // save() serialises the Basket (and its List<BasketItem>) to a Redis hash
        Basket savedBasket = basketRepository.save(basket);
        log.info("Basket created by Id : {}", savedBasket.getId());
        // Convert the saved entity back to a DTO for the HTTP response
        return convertToBasketResponse(savedBasket);
    }

    /**
     * convertToBasketResponse — maps a Basket Redis entity to a BasketResponse DTO.
     *
     * Converts each BasketItem in the basket using convertToBasketItemResponse.
     * Returns null if the basket itself is null (defensive check for callers that
     * may pass null, e.g. getBasketById when the basket doesn't exist).
     *
     * @param basket the Basket entity from Redis (may be null)
     * @return a BasketResponse DTO, or null if basket is null
     */
    private BasketResponse convertToBasketResponse(Basket basket) {
        if (basket == null) {
            return null; // propagate null — callers handle this case
        }
        // Convert each BasketItem entity to a BasketItemResponse DTO
        List<BasketItemResponse> itemResponses = basket.getItems().stream()
                .map(this::convertToBasketItemResponse)
                .collect(Collectors.toList());
        return BasketResponse.builder()
                .id(basket.getId())
                .items(itemResponses)
                .build();
    }

    /**
     * convertToBasketItemResponse — maps a single BasketItem Redis entity to its DTO.
     *
     * All fields are copied directly. productBrand and productType are already
     * stored as strings in the entity (denormalised), so no further resolution is needed.
     *
     * @param basketItem the BasketItem entity from Redis
     * @return a BasketItemResponse DTO safe to serialise to JSON
     */
    private BasketItemResponse convertToBasketItemResponse(BasketItem basketItem) {
        return BasketItemResponse.builder()
                .id(basketItem.getId())
                .name(basketItem.getName())
                .description(basketItem.getDescription())
                .price(basketItem.getPrice())
                .pictureUrl(basketItem.getPictureUrl())
                .productBrand(basketItem.getProductBrand()) // already a String — no JOIN needed
                .productType(basketItem.getProductType())   // already a String — no JOIN needed
                .quantity(basketItem.getQuantity())
                .build();
    }
}
