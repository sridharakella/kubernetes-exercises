import { Product } from "./product";

/**
 * ProductData interface — the shape of Spring Boot's Page<ProductResponse> JSON response.
 *
 * Spring's Page<T> returns a rich pagination envelope. This interface captures the three
 * fields Angular actually uses; unused Spring Page fields (sort, empty, first, last, etc.)
 * are simply ignored by TypeScript's structural typing.
 *
 * Example response from GET /api/products?page=0&size=10:
 * {
 *   "content": [ { "id": 1, "name": "...", ... }, ... ],  // Array of products for this page
 *   "pageable": { "pageNumber": 0, "pageSize": 10 },      // Current page info (0-based)
 *   "totalElements": 70                                    // Total matching products across all pages
 * }
 *
 * StoreComponent.fetchProducts() maps this:
 *   data.content       → storeData.products  (the visible product cards)
 *   data.pageable      → storeData.pageable  (used to read pageNumber for currentPage)
 *   data.totalElements → storeData.totalElements (used by pagination component to compute page count)
 */
export interface ProductData {
  /** Current page's product array — displayed as product cards in the store grid */
  content: Product[];

  /** Spring's Pageable metadata — pageNumber is 0-based; add 1 to get the UI page number */
  pageable: {
    pageNumber: number; // 0-based page index (Spring convention)
    pageSize: number;   // Items per page (matches the ?size= query param)
  };

  /** Total count of products matching the current filters (used to calculate total pages) */
  totalElements: number;
}
