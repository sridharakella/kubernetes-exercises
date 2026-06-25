package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BrandResponse — lightweight DTO for a product brand.
 *
 * Returned by GET /api/products/brands. The Angular store component uses
 * this list to build the brand filter sidebar. Only id and name are exposed —
 * the List<Product> association on the Brand entity is intentionally omitted
 * to keep the response small and avoid serialisation issues.
 *
 * The Angular Brand interface mirrors this shape exactly:
 *   export interface Brand { id: number; name: string; }
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BrandResponse {
    private Integer id;   // Brand primary key — used as the filter parameter (?brandId=1)
    private String name;  // Brand display name shown in the filter sidebar (e.g. "Nike", "Yonex")
}
