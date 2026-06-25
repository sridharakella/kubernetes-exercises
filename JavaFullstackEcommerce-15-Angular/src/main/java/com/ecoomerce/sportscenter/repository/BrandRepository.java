package com.ecoomerce.sportscenter.repository;

import com.ecoomerce.sportscenter.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * BrandRepository — Spring Data JPA repository for the Brand MySQL table.
 *
 * Extends JpaRepository<Brand, Integer> which provides all standard CRUD operations
 * and pagination support automatically — no SQL or implementation code needed:
 *   - findAll()          → SELECT * FROM Brand
 *   - findById(id)       → SELECT * FROM Brand WHERE Id = ?
 *   - save(brand)        → INSERT or UPDATE
 *   - deleteById(id)     → DELETE FROM Brand WHERE Id = ?
 *
 * No custom @Query methods are needed here — BrandService only needs findAll().
 *
 * @Repository : marks this as a Spring Data repository; also converts SQL exceptions
 *               to Spring's DataAccessException hierarchy for consistent error handling
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    // All required methods are inherited from JpaRepository — no custom queries needed
}
