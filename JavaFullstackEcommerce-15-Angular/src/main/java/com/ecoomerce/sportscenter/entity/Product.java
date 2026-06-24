package com.ecoomerce.sportscenter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product — JPA entity representing a sports product in the MySQL database.
 *
 * Maps to the "Product" table in the sportscenter MySQL schema.
 * Each product belongs to one Brand and one Type (many-to-one relationships).
 *
 * Lombok annotations used:
 *   @Data          : auto-generates getters, setters, equals, hashCode, and toString
 *   @AllArgsConstructor : generates a constructor with all fields as parameters
 *   @NoArgsConstructor  : generates a no-arg constructor (required by JPA)
 *   @Builder       : enables the builder pattern for clean object construction
 *                    e.g. Product.builder().name("Racket").price(5000L).build()
 *
 * @Entity : tells JPA this class maps to a database table
 * @Table  : explicitly names the target table (avoids case-sensitivity issues)
 */
@Entity
@Table(name="Product")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

    @Id                                                     // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // Auto-increment in MySQL (AUTO_INCREMENT)
    @Column(name="Id")                                     // Maps to the "Id" column in the Product table
    private Integer id;

    @Column(name="Name")                                   // Maps to the "Name" column
    private String name;                                   // Product display name (e.g. "Yonex Badminton Racket")

    @Column(name="Description")                            // Maps to the "Description" column
    private String description;                            // Detailed product description

    @Column(name="Price")                                  // Maps to the "Price" column
    private Long price;                                    // Price stored as Long (in smallest currency unit, e.g. paise/cents)

    @Column(name="PictureUrl")                             // Maps to the "PictureUrl" column
    private String pictureUrl;                             // Relative or absolute URL to the product image

    /**
     * Many products can belong to one Brand (many-to-one relationship).
     *
     * fetch = FetchType.LAZY : the Brand is NOT loaded from the database until brand.getName()
     *                          (or similar) is first called — avoids unnecessary JOIN queries
     * @JoinColumn             : the "ProductBrandId" column in the Product table is the foreign key
     *                          referencing Brand.Id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "ProductBrandId", referencedColumnName = "Id")
    private Brand brand;

    /**
     * Many products can belong to one Type (many-to-one relationship).
     *
     * fetch = FetchType.LAZY : the Type is loaded lazily (on first access)
     * @JoinColumn             : "ProductTypeId" in the Product table references Type.Id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "ProductTypeId", referencedColumnName = "Id")
    private Type type;
}
