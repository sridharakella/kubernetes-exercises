package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.entity.Product;
import com.ecoomerce.sportscenter.exceptions.ProductNotFoundException;
import com.ecoomerce.sportscenter.model.ProductResponse;
import com.ecoomerce.sportscenter.repository.ProductRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductServiceImpl — concrete implementation of ProductService.
 *
 * Responsibilities:
 *   1. Call the correct ProductRepository method based on which filters are active
 *   2. Convert Product JPA entities to ProductResponse DTOs before returning
 *   3. Throw ProductNotFoundException when a requested product doesn't exist
 *
 * The entity-to-DTO conversion (convertToProductResponse) is critical:
 *   - It resolves the lazy-loaded Brand and Type associations (product.getBrand().getName())
 *   - It exposes brand/type as plain Strings, not nested objects — preventing Jackson
 *     from serialising the full Brand entity (which includes a List<Product> → infinite recursion)
 *
 * @Service  : registers this as a Spring-managed singleton service bean
 * @Log4j2   : injects a Log4j2 logger via Lombok for structured logging
 */
@Service
@Log4j2
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository; // Spring Data JPA repository for MySQL

    /**
     * Constructor injection — Spring injects the ProductRepository dependency.
     *
     * @param productRepository provides all product DB operations
     */
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * {@inheritDoc}
     *
     * Uses Optional.orElseThrow() to surface a clear 404 error through
     * CustomExceptionHandler when the product ID doesn't exist in MySQL.
     */
    @Override
    public ProductResponse getProductById(Integer productId) {
        log.info("Fetching Product by Id: {}", productId);
        // findById returns Optional<Product>; orElseThrow gives a meaningful error if absent
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with given id doesn't exist"));
        // Convert JPA entity → DTO before returning (resolves lazy Brand/Type associations)
        ProductResponse productResponse = convertToProductResponse(product);
        log.info("Fetched Product by Id: {}", productId);
        return productResponse;
    }

    /**
     * {@inheritDoc}
     *
     * Uses Spring Data's Page.map() to apply the entity→DTO conversion to every
     * element in the page without loading all products into memory at once.
     */
    @Override
    public Page<ProductResponse> getProducts(Pageable pageable) {
        log.info("Fetching products");
        // findAll(pageable) executes a paginated SELECT with ORDER BY from the Pageable
        Page<Product> productPage = productRepository.findAll(pageable);
        // Page.map() converts each Product entity to ProductResponse, preserving pagination metadata
        Page<ProductResponse> productResponses = productPage
                .map(this::convertToProductResponse);
        log.info("Fetched all products");
        return productResponses;
    }

    /**
     * {@inheritDoc}
     *
     * Streams the result list and maps each entity to a DTO.
     * Collectors.toList() materialises the stream back into a List.
     */
    @Override
    public List<ProductResponse> searchProductsByName(String keyword) {
        log.info("Searching product(s) by name: {}", keyword);
        // JPQL: SELECT p FROM Product p WHERE p.name LIKE %:keyword%
        List<Product> products = productRepository.searchByName(keyword);
        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)  // entity → DTO for each product
                .collect(Collectors.toList());
        log.info("Fetched all products");
        return productResponses;
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductResponse> searchProductsByBrand(Integer brandId) {
        log.info("Searching product(s) by brandId: {}", brandId);
        // JPQL: SELECT p FROM Product p WHERE p.brand.id = :brandId
        List<Product> products = productRepository.searchByBrand(brandId);
        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
        log.info("Fetched all products");
        return productResponses;
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductResponse> searchProductsByType(Integer typeId) {
        log.info("Searching product(s) by typeId: {}", typeId);
        // JPQL: SELECT p FROM Product p WHERE p.type.id = :typeId
        List<Product> products = productRepository.searchByType(typeId);
        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
        log.info("Fetched all products");
        return productResponses;
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductResponse> searchProductsByBrandandType(Integer brandId, Integer typeId) {
        log.info("Searching product(s) by brandId: {}, and typeId: {}", brandId, typeId);
        // JPQL: SELECT p FROM Product p WHERE p.brand.id=:brandId AND p.type.id=:typeId
        List<Product> products = productRepository.searchByBrandAndType(brandId, typeId);
        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
        log.info("Fetched all products");
        return productResponses;
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductResponse> searchProductsByBrandTypeAndName(Integer brandId, Integer typeId, String keyword) {
        log.info("Searching product(s) by brandId: {}, typeId: {} and keyword: {}", brandId, typeId, keyword);
        // JPQL: SELECT p FROM Product p WHERE p.brand.id=:brandId AND p.type.id=:typeId AND p.name LIKE %:keyword%
        List<Product> products = productRepository.searchByBrandTypeAndName(brandId, typeId, keyword);
        List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
        log.info("Fetched all products");
        return productResponses;
    }

    /**
     * convertToProductResponse — maps a Product JPA entity to a ProductResponse DTO.
     *
     * Key decisions:
     *   - product.getType().getName()  resolves the lazy-loaded Type (triggers a SELECT if not cached)
     *   - product.getBrand().getName() resolves the lazy-loaded Brand (same)
     *   - Brand and Type are exposed as plain Strings — avoids bidirectional serialisation issues
     *
     * Uses the Lombok @Builder pattern for concise, readable object construction.
     *
     * @param product the JPA entity from MySQL
     * @return a ProductResponse DTO safe to serialise to JSON
     */
    private ProductResponse convertToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .pictureUrl(product.getPictureUrl())
                .productType(product.getType().getName())   // resolve lazy Type association → String
                .productBrand(product.getBrand().getName()) // resolve lazy Brand association → String
                .build();
    }
}
