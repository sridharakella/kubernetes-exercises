package com.ecoomerce.sportscenter.service;

import com.ecoomerce.sportscenter.model.TypeResponse;

import java.util.List;

/**
 * TypeService — service interface for product type/category lookup operations.
 *
 * Types (e.g. Shoes, Rackets, Gloves) are used as filter options in the Angular
 * store sidebar alongside brands. The concrete implementation is TypeServiceImpl.
 */
public interface TypeService {

    /**
     * Retrieve all product types from MySQL.
     *
     * @return list of all TypeResponse DTOs (id + name for each type)
     */
    List<TypeResponse> getAllTypes();
}
