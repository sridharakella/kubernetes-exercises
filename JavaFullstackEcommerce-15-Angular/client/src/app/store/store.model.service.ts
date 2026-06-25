import { Injectable } from '@angular/core';
import { Brand } from '../shared/models/brand';
import { Product } from '../shared/models/product';
import { StoreData } from '../shared/models/storeData';
import { Type } from '../shared/models/type';

/**
 * StoreModelService — singleton state container for the store page.
 *
 * This service acts as a simple state store for all filter, sort, search,
 * and pagination state used by the StoreComponent.
 *
 * Why a service instead of component properties?
 *   - Angular destroys components when you navigate away (e.g. /store → /basket → /store).
 *   - Component properties are lost when the component is destroyed.
 *   - A singleton service survives navigation — the user's filters, search term, and
 *     current page are all preserved when they return to the store.
 *
 * Implements StoreData interface to enforce the shape of the state contract.
 * StoreComponent injects this service as `public storeData` so the template
 * can bind directly: *ngFor="let p of storeData.products"
 */
@Injectable({
  providedIn: 'root' // singleton — shared across the entire application
})
export class StoreModelService implements StoreData {
  /** Current page of product results (fetched from the API) */
  products: Product[] = [];

  /** All available brands — populated once on StoreComponent init */
  brands: Brand[] = [];

  /** All available product types — populated once on StoreComponent init */
  types: Type[] = [];

  /** Currently selected brand filter (null or {id:0, name:'All'} = no filter) */
  selectedBrand: Brand | null = null;

  /** Currently selected type filter (null or {id:0, name:'All'} = no filter) */
  selectedType: Type | null = null;

  /** Current sort direction — 'asc' or 'desc' (default: ascending by name) */
  selectedSort = 'asc';

  /** Current keyword search term (empty string = no keyword filter) */
  search = '';

  /** Currently displayed page number (1-based for the UI pagination component) */
  currentPage = 1;

  /** Optional raw page number (mirrors currentPage; may be undefined initially) */
  page?: number;

  /** Raw pageable metadata object from the Spring Page<T> response (pageNumber, pageSize) */
  pageable: any;

  /** Total number of products matching the current filters (from Spring totalElements field) */
  totalElements: number = 70;

  /** Number of products to display per page (maps to Spring's ?size= query param) */
  pageSize: number = 10;
}
