package com.ecoomerce.sportscenter.controller;

import com.ecoomerce.sportscenter.model.BrandResponse;
import com.ecoomerce.sportscenter.model.ProductResponse;
import com.ecoomerce.sportscenter.model.TypeResponse;
import com.ecoomerce.sportscenter.service.BrandService;
import com.ecoomerce.sportscenter.service.ProductService;
import com.ecoomerce.sportscenter.service.TypeService;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProductController — REST controller for all product-related endpoints.
 *
 * Base URL: /api/products
 *
 * Endpoints:
 *   GET  /api/products/{id}      — fetch a single product by its ID
 *   GET  /api/products           — paginated product listing with optional filters/sort
 *   GET  /api/products/brands    — list all brands (for filter sidebar)
 *   GET  /api/products/types     — list all types/categories (for filter sidebar)
 *   GET  /api/products/search    — keyword search shortcut (delegates to getProducts)
 *
 * The getProducts endpoint applies a priority cascade for filter combinations:
 *   brandId + typeId + keyword → most specific search (all three)
 *   brandId + typeId           → brand and type filter
 *   brandId only               → brand filter
 *   typeId only                → type filter
 *   keyword only               → name substring search
 *   none                       → all products with sort/pagination
 *
 * @RestController : combines @Controller + @ResponseBody — methods return JSON automatically
 * @RequestMapping : all routes prefixed with "/api/products"
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService; // product CRUD and search operations
    private final TypeService typeService;        // type/category listing
    private final BrandService brandService;      // brand listing

    /**
     * Constructor injection — Spring wires all three service dependencies.
     *
     * @param productService handles product lookup, pagination, and search
     * @param typeService    handles type listing
     * @param brandService   handles brand listing
     */
    public ProductController(ProductService productService, TypeService typeService, BrandService brandService) {
        this.productService = productService;
        this.typeService = typeService;
        this.brandService = brandService;
    }

    /**
     * getProductById — GET /api/products/{id}
     *
     * Fetches a single product by its primary key. Throws ProductNotFoundException
     * (handled by CustomExceptionHandler → 404 response) if the ID does not exist.
     *
     * @param productId the product's primary key (from the URL path)
     * @return 200 OK with the ProductResponse DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") Integer productId){
        ProductResponse productResponse = productService.getProductById(productId);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    /**
     * getProducts — GET /api/products
     *
     * Returns a paginated product list. Supports optional filters:
     *   ?keyword=racket   — name contains "racket"
     *   ?brandId=1        — products from brand with id=1
     *   ?typeId=2         — products of type with id=2
     *   ?sort=name        — field to sort by (default: "name")
     *   ?order=asc|desc   — sort direction (default: "asc")
     *   ?page=0&size=10   — pagination (Spring Pageable)
     *
     * Priority cascade: all three > brand+type > brand only > type only > keyword only > default.
     * Filtered results are wrapped in PageImpl to preserve the Page<T> contract expected by Angular.
     *
     * @param pageable    Spring-injected pagination info (page, size) from query params
     * @param keyword     optional name search term
     * @param brandId     optional brand filter
     * @param typeId      optional type/category filter
     * @param sort        field to sort by when using the default full-list path (default: "name")
     * @param order       sort direction: "asc" or "desc" (default: "asc")
     * @return 200 OK with a Page<ProductResponse> (content array + pagination metadata)
     */
    @GetMapping()
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(name="keyword", required = false) String keyword,
            @RequestParam(name="brandId", required = false) Integer brandId,
            @RequestParam(name="typeId", required = false) Integer typeId,
            @RequestParam(name="sort", defaultValue = "name") String sort,
            @RequestParam(name = "order", defaultValue = "asc") String order
    ){
        Page<ProductResponse> productResponsePage;

        if(brandId!=null && typeId!=null && keyword!=null && !keyword.isEmpty()) {
            // Most specific filter: brand + type + keyword all provided
            List<ProductResponse> productResponses = productService.searchProductsByBrandTypeAndName(brandId, typeId, keyword);
            // Wrap List in PageImpl so the response shape matches the paginated case
            productResponsePage = new PageImpl<>(productResponses, pageable, productResponses.size());
        }
        else if(brandId!=null && typeId!=null) {
            // Brand and type filter (no keyword)
            List<ProductResponse> productResponses = productService.searchProductsByBrandandType(brandId, typeId);
            productResponsePage = new PageImpl<>(productResponses, pageable, productResponses.size());
        }
        else if(brandId!=null) {
            // Brand filter only
            List<ProductResponse> productResponses = productService.searchProductsByBrand(brandId);
            productResponsePage = new PageImpl<>(productResponses, pageable, productResponses.size());
        }
        else if(typeId!=null) {
            // Type/category filter only
            List<ProductResponse> productResponses = productService.searchProductsByType(typeId);
            productResponsePage = new PageImpl<>(productResponses, pageable, productResponses.size());
        }
        else if(keyword!=null && !keyword.isEmpty()){
            // Keyword-only search (name LIKE %keyword%)
            List<ProductResponse> productResponses = productService.searchProductsByName(keyword);
            productResponsePage = new PageImpl<>(productResponses, pageable, productResponses.size());
        } else {
            // Default: no filters — return all products with sort applied
            // Build Sort from query params: e.g. Sort.by(ASC, "name")
            Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sorting = Sort.by(direction, sort);
            // Override pageable with the explicit sort (PageableDefault only sets size, not sort)
            productResponsePage = productService.getProducts(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting));
        }
        return new ResponseEntity<>(productResponsePage, HttpStatus.OK);
    }

    /**
     * getBrands — GET /api/products/brands
     *
     * Returns all brands. Used by the Angular store sidebar to populate the brand filter list.
     *
     * @return 200 OK with a List<BrandResponse> containing id and name for each brand
     */
    @GetMapping("/brands")
    public ResponseEntity<List<BrandResponse>> getBrands(){
        List<BrandResponse> brandResponses = brandService.getAllBrands();
        return new ResponseEntity<>(brandResponses, HttpStatus.OK);
    }

    /**
     * getTypes — GET /api/products/types
     *
     * Returns all product types/categories. Used by the Angular store sidebar to populate the type filter.
     *
     * @return 200 OK with a List<TypeResponse> containing id and name for each type
     */
    @GetMapping("/types")
    public ResponseEntity<List<TypeResponse>> getTypes(){
        List<TypeResponse> typeResponses = typeService.getAllTypes();
        return new ResponseEntity<>(typeResponses, HttpStatus.OK);
    }

    /**
     * searchProducts — GET /api/products/search?keyword=
     *
     * Convenience endpoint that delegates to searchProductsByName.
     * Returns a flat List (not paginated) for simple keyword searches.
     *
     * @param keyword the search term to match against product names (substring match)
     * @return 200 OK with a List<ProductResponse> of matching products
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam("keyword") String keyword){
        List<ProductResponse> productResponses = productService.searchProductsByName(keyword);
        return new ResponseEntity<>(productResponses, HttpStatus.OK);
    }
}
