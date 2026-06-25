package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.entity.Brand;
import com.ecoomerce.sportscenter.model.BrandResponse;
import com.ecoomerce.sportscenter.repository.BrandRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BrandServiceImpl — concrete implementation of BrandService.
 *
 * Fetches all brands from MySQL via BrandRepository and converts each
 * Brand JPA entity to a BrandResponse DTO. Brands are lookup data (rarely change)
 * and are fetched once on Angular page load to populate the store filter sidebar.
 *
 * @Service : registers this as a Spring-managed singleton service bean
 * @Log4j2  : injects a Log4j2 logger via Lombok
 */
@Service
@Log4j2
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository; // Spring Data JPA repository for the Brand table

    /**
     * Constructor injection — Spring injects the BrandRepository dependency.
     *
     * @param brandRepository provides findAll() and other CRUD operations for brands
     */
    public BrandServiceImpl(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    /**
     * {@inheritDoc}
     *
     * Executes a SELECT * FROM Brand query and converts each row to a BrandResponse DTO.
     * The List<Product> association on Brand is lazy-loaded and NOT accessed here,
     * so no extra SQL is executed for product relationships.
     */
    @Override
    public List<BrandResponse> getAllBrands() {
        log.info("Fetching all Brands!!!");
        // findAll() executes: SELECT Id, Name FROM Brand
        List<Brand> brandList = brandRepository.findAll();
        // Stream: convert each Brand entity → BrandResponse DTO (only id + name exposed)
        List<BrandResponse> brandResponses = brandList.stream()
                .map(this::convertToBrandResponse)
                .collect(Collectors.toList());
        log.info("Fetched all Brands");
        return brandResponses;
    }

    /**
     * convertToBrandResponse — maps a Brand JPA entity to a BrandResponse DTO.
     *
     * Only exposes id and name — does NOT include the List<Product> association.
     * This keeps the API response lightweight and prevents serialisation of the full
     * product list for every brand.
     *
     * @param brand the Brand entity from MySQL
     * @return a BrandResponse DTO with id and name only
     */
    private BrandResponse convertToBrandResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .build();
    }
}
