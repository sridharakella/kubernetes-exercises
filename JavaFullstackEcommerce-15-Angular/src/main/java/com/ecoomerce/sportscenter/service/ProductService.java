package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.model.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ProductService — service interface defining the product business operations contract.
 *
 * Programming to an interface decouples the controller from the implementation.
 * Benefits:
 *   - Easy to swap implementations (e.g. swap MySQL for Elasticsearch) without touching controllers
 *   - Enables mocking in unit tests (Mockito can mock any interface)
 *   - Enforces a clear API boundary between the HTTP layer and the business logic layer
 *
 * The concrete implementation is ProductServiceImpl.
 */
public interface ProductService {

    /**
     * Fetch a single product by its primary key.
     * Throws ProductNotFoundException if the ID does not exist.
     */
    ProductResponse getProductById(Integer productId);

    /**
     * Fetch all products with pagination and sorting applied.
     * Used for the default product listing (no filters active).
     *
     * @param pageable Spring Data pagination/sort descriptor (page, size, sort)
     * @return a Page containing the current page of ProductResponse DTOs + pagination metadata
     */
    Page<ProductResponse> getProducts(Pageable pageable);

    /**
     * Search products whose name contains the given keyword (case-insensitive LIKE).
     */
    List<ProductResponse> searchProductsByName(String keyword);

    /**
     * Filter products by brand ID (exact match on brand foreign key).
     */
    List<ProductResponse> searchProductsByBrand(Integer brandId);

    /**
     * Filter products by type/category ID (exact match on type foreign key).
     */
    List<ProductResponse> searchProductsByType(Integer typeId);

    /**
     * Filter products by both brand AND type simultaneously.
     * Used when the user has selected both a brand and a type in the store sidebar.
     */
    List<ProductResponse> searchProductsByBrandandType(Integer brandId, Integer typeId);

    /**
     * Filter products by brand, type, AND keyword — the most specific search.
     * Used when the user has active brand/type filters AND has typed a search term.
     */
    List<ProductResponse> searchProductsByBrandTypeAndName(Integer brandId, Integer typeId, String keyword);
}
