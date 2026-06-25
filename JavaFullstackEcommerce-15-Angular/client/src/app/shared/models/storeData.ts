import { Brand } from "./brand";
import { Product } from "./product";
import { Type } from "./type";

/**
 * StoreData interface — defines the shape of the store page's UI state contract.
 *
 * StoreModelService implements this interface and holds the actual values.
 * The interface exists to:
 *   1. Document every piece of state the store page needs
 *   2. Enforce the type contract between StoreComponent (consumer) and StoreModelService (provider)
 *   3. Allow alternative implementations (e.g. for testing) that satisfy the same shape
 *
 * StoreComponent injects StoreModelService as `public storeData: StoreModelService`
 * and the template binds to it directly: *ngFor="let p of storeData.products"
 */
export interface StoreData {
  /** Current page of products fetched from the API */
  products: Product[];

  /** All brands for the filter sidebar (includes "All" entry at index 0 with id: 0) */
  brands: Brand[];

  /** All product types for the filter sidebar (includes "All" entry at index 0 with id: 0) */
  types: Type[];

  /** Currently active brand filter; {id:0, name:'All'} means no brand filter applied */
  selectedBrand: Brand | null;

  /** Currently active type filter; {id:0, name:'All'} means no type filter applied */
  selectedType: Type | null;

  /** Current sort order — 'asc' | 'desc' (default: 'asc', maps to ?order= query param) */
  selectedSort: string;

  /** Current keyword search term ('' = no keyword filter applied) */
  search: string;

  /** Current page number (1-based for the UI; subtract 1 when sending to Spring's ?page= param) */
  currentPage: number;

  /** Raw page number (may mirror currentPage; optional, used during URL state construction) */
  page?: number;

  /** Raw pageable object from Spring's Page<T> response — used to extract pageNumber */
  pageable: any;

  /** Total number of matching products (from Spring's totalElements — drives pagination) */
  totalElements: number;

  /** Products per page (maps to Spring's ?size= query param; default: 10) */
  pageSize: number;
}
