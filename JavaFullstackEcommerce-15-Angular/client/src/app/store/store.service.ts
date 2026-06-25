import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ProductData } from '../shared/models/productData';
import { HttpClient } from '@angular/common/http';
import { Brand } from '../shared/models/brand';
import { Product } from '../shared/models/product';

/**
 * StoreService — handles all HTTP calls to the product catalog API.
 *
 * This service is the single point of contact between the Angular store UI and
 * the Spring Boot /api/products endpoints. The StoreComponent builds the full
 * query URL (with filters, pagination, sort) and passes it here.
 *
 * @Injectable({ providedIn: 'root' }) — Angular creates a singleton shared across
 * the whole app without adding it to any module's providers array.
 */
@Injectable({
  providedIn: 'root'
})
export class StoreService {

  /** Base URL for the Spring Boot products API. Public so StoreComponent can build query URLs. */
  public apiUrl = 'http://localhost:8080/api/products';

  constructor(private http: HttpClient) { }

  /**
   * getProducts — fetches a paginated, optionally filtered list of products.
   *
   * StoreComponent.fetchProducts() builds the full query URL with all filter/page/sort
   * parameters and passes it as `url`. This keeps URL construction in the component
   * (where filter state lives) and HTTP calls here.
   *
   * @param brandId optional brand filter (for context; URL construction is in the component)
   * @param typeId  optional type filter (for context)
   * @param url     pre-built full query URL with filter, page, and sort params
   * @returns Observable<ProductData> matching the Spring Page<ProductResponse> JSON shape
   */
  getProducts(brandId?: number, typeId?: number, url?: string): Observable<ProductData> {
    // Use the provided URL if supplied, otherwise fall back to the bare base URL
    const apiUrl = url || this.apiUrl;
    return this.http.get<ProductData>(apiUrl);
  }

  /**
   * getProduct — fetches a single product by its ID.
   *
   * Called by ProductDetailsComponent to load the full product detail page.
   *
   * @param id the product's primary key
   * @returns Observable<Product>
   */
  getProduct(id: number) {
    return this.http.get<Product>(this.apiUrl + '/' + id);
  }

  /**
   * getBrands — fetches all brands for the store filter sidebar.
   *
   * Called once on StoreComponent.ngOnInit(). StoreComponent prepends an "All"
   * entry (id: 0, name: 'All') so users can deselect the brand filter.
   *
   * @returns Observable<Brand[]>
   */
  getBrands() {
    const url = `${this.apiUrl}/brands`;
    return this.http.get<Brand[]>(url);
  }

  /**
   * getTypes — fetches all product types/categories for the store filter sidebar.
   *
   * Note: returns Brand[] shape — types share the same {id, name} structure as brands.
   *
   * @returns Observable<Brand[]> (same {id, name} shape as Brand)
   */
  getTypes() {
    const url = `${this.apiUrl}/types`;
    return this.http.get<Brand[]>(url);
  }
}
