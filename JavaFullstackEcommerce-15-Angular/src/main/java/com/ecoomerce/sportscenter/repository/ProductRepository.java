package com.ecoomerce.sportscenter.repository;

import com.ecoomerce.sportscenter.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ProductRepository — Spring Data JPA repository for Product entity.
 *
 * Extends JpaRepository<Product, Integer> which automatically provides:
 *   - findById(Integer id)          → Optional<Product>
 *   - findAll()                     → List<Product>
 *   - findAll(Pageable pageable)    → Page<Product>  (used for paginated product listing)
 *   - save(Product product)         → Product
 *   - deleteById(Integer id)        → void
 *
 * All custom query methods below use JPQL (Java Persistence Query Language),
 * which works on entity class names and field names — NOT raw SQL table/column names.
 * Hibernate translates JPQL to the appropriate MySQL SQL at runtime.
 *
 * @Repository : marks this as a Spring Data repository bean;
 *               also converts database exceptions to Spring's DataAccessException hierarchy
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    /**
     * searchByName — full-text LIKE search on the product name field.
     *
     * JPQL: SELECT p FROM Product p WHERE p.name LIKE %:keyword%
     *   - %:keyword% wraps the keyword in wildcards → matches any substring position
     *   - Case sensitivity depends on the MySQL collation (typically case-insensitive by default)
     *
     * @param keyword the search term entered by the user
     * @return list of products whose name contains the keyword
     */
    @Query("SELECT p FROM Product p where p.name LIKE %:keyword%")
    List<Product> searchByName(@Param("keyword") String keyword);

    /**
     * searchByBrand — filters products by brand ID.
     *
     * Navigates the Product → Brand relationship using p.brand.id
     * (JPQL traverses the @ManyToOne association automatically).
     *
     * @param brandId the primary key of the Brand to filter by
     * @return list of products belonging to the specified brand
     */
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId")
    List<Product> searchByBrand(@Param("brandId") Integer brandId);

    /**
     * searchByType — filters products by type/category ID.
     *
     * @param typeId the primary key of the Type to filter by
     * @return list of products belonging to the specified type
     */
    @Query("SELECT p FROM Product p WHERE p.type.id = :typeId")
    List<Product> searchByType(@Param("typeId") Integer typeId);

    /**
     * searchByBrandAndType — filters products by both brand and type simultaneously.
     *
     * Used when the user selects a brand filter AND a type filter in the store UI.
     *
     * @param brandId the brand filter
     * @param typeId  the type/category filter
     * @return list of products matching both the brand and type criteria
     */
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId AND p.type.id = :typeId")
    List<Product> searchByBrandAndType(@Param("brandId") Integer brandId, @Param("typeId") Integer typeId);

    /**
     * searchByBrandTypeAndName — filters products by brand, type, AND name keyword.
     *
     * The most specific search — combines all three filter dimensions.
     * Used when the user has brand/type filters active AND types a search keyword.
     *
     * @param brandId the brand filter
     * @param typeId  the type/category filter
     * @param keyword the name search term (matched as a substring)
     * @return list of products matching brand, type, and name criteria
     */
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId AND p.type.id = :typeId AND p.name LIKE %:keyword%")
    List<Product> searchByBrandTypeAndName(@Param("brandId") Integer brandId, @Param("typeId") Integer typeId, @Param("keyword") String keyword);
}
