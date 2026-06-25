package com.ecoomerce.sportscenter.repository;

import com.ecoomerce.sportscenter.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TypeRepository — Spring Data JPA repository for the Type MySQL table.
 *
 * Mirrors BrandRepository in structure and purpose. Provides standard CRUD
 * operations for product types/categories (Shoes, Rackets, Gloves, etc.)
 * with no custom queries needed — TypeService only calls findAll().
 *
 * Free operations inherited from JpaRepository<Type, Integer>:
 *   - findAll()          → SELECT * FROM Type
 *   - findById(id)       → SELECT * FROM Type WHERE Id = ?
 *   - save(type)         → INSERT or UPDATE
 *   - deleteById(id)     → DELETE FROM Type WHERE Id = ?
 *
 * @Repository : declares this as a Spring Data repository bean
 */
@Repository
public interface TypeRepository extends JpaRepository<Type, Integer> {
    // All required methods are inherited from JpaRepository — no custom queries needed
}
