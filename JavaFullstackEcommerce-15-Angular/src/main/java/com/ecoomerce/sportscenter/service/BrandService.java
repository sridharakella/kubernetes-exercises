package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.model.BrandResponse;

import java.util.List;

/**
 * BrandService — service interface for brand lookup operations.
 *
 * Brands are used as filter options in the Angular store sidebar.
 * The full list is fetched once on page load and displayed as clickable filters.
 *
 * The concrete implementation is BrandServiceImpl.
 */
public interface BrandService {

    /**
     * Retrieve all brands from MySQL.
     *
     * @return list of all BrandResponse DTOs (id + name for each brand)
     */
    List<BrandResponse> getAllBrands();
}
