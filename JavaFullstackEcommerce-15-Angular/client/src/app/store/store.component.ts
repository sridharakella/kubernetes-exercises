import { Component, Input, OnInit } from '@angular/core';
import { StoreService } from './store.service';
import { Product } from '../shared/models/product';
import { Brand } from '../shared/models/brand';
import { Type } from '../shared/models/type';
import { PageChangedEvent } from 'ngx-bootstrap/pagination';
import { StoreModelService } from './store.model.service';
import { ToastrService } from 'ngx-toastr';

/**
 * StoreComponent — the main product catalog page.
 *
 * Displays the product grid with:
 *   - Brand filter sidebar (left)
 *   - Type/category filter sidebar (left, below brands)
 *   - Sort dropdown (top-right)
 *   - Keyword search bar (top)
 *   - Paginated product cards (centre)
 *
 * State is held in StoreModelService (a singleton) — not in this component —
 * so filter/search/page selections persist when the user navigates away and returns.
 *
 * On any filter, sort, search, or page change, fetchProducts() rebuilds the query URL
 * from the current state and calls the API.
 */
@Component({
  selector: 'app-store',
  templateUrl: './store.component.html',
  styleUrls: ['./store.component.scss'],
})
export class StoreComponent implements OnInit {

  constructor(
    private storeService: StoreService,  // HTTP calls to /api/products
    public storeData: StoreModelService, // Singleton state (public so template can bind directly)
    private toastr: ToastrService        // Success/error toast notifications
  ) {}

  /** Optional title input (e.g. when used as a child component on the home page) */
  @Input() title: string = '';

  /**
   * ngOnInit — initialises the store on component creation.
   *
   * Resets brand and type selection to "All" (id: 0) on every navigation to /store,
   * then kicks off three parallel data loads:
   *   1. fetchProducts() — loads the first page of products
   *   2. getBrands()     — loads brand filter options for the sidebar
   *   3. getTypes()      — loads type filter options for the sidebar
   */
  ngOnInit() {
    // Reset filters to "All" on page enter
    this.storeData.selectedBrand = { id: 0, name: 'All' };
    this.storeData.selectedType = { id: 0, name: 'All' };

    this.fetchProducts(); // Load the first page of products (no filters)
    this.getBrands();     // Populate brand filter sidebar
    this.getTypes();      // Populate type filter sidebar
  }

  /**
   * pageChanged — handles the pagination component's page change event.
   *
   * Called by the ngx-bootstrap pagination component (pagination-header template).
   * Only re-fetches if the page number actually changed to avoid duplicate API calls.
   *
   * @param event contains event.page — the new page number (1-based)
   */
  pageChanged(event: PageChangedEvent): void {
    if (event.page !== this.storeData.currentPage) {
      this.storeData.currentPage = event.page;
      this.fetchProducts(this.storeData.currentPage);
    }
  }

  /**
   * fetchProducts — builds the API query URL from current state and fetches products.
   *
   * This is the central fetch method called by all filter, sort, search, and page actions.
   * It reads the current state from StoreModelService and constructs a parameterised URL.
   *
   * URL construction example:
   *   /api/products?brandId=1&typeId=2&keyword=racket&page=0&size=10&sort=name&order=desc
   *
   * Page number conversion:
   *   UI pages are 1-based (page 1 = first page for the user)
   *   Spring Boot pages are 0-based (?page=0 = first page)
   *   So backendPage = page - 1
   *
   * On success: updates storeData with the response data and adjusts currentPage
   *             (Spring returns pageable.pageNumber which is 0-based → add 1 for UI)
   *
   * @param page the current UI page number (1-based, default: 1)
   */
  fetchProducts(page: number = 1) {
    const backendPage = page - 1; // Convert 1-based UI page to 0-based Spring page

    const brandId = this.storeData.selectedBrand?.id; // undefined if no brand selected
    const typeId = this.storeData.selectedType?.id;   // undefined if no type selected

    // Build the query URL incrementally from current filter state
    let url = `${this.storeService.apiUrl}?`;

    // Only append brandId if a real brand is selected (not the "All" placeholder with id 0)
    if (brandId && brandId !== 0) {
      url += `brandId=${brandId}&`;
    }

    // Only append typeId if a real type is selected
    if (typeId && typeId !== 0) {
      url += `typeId=${typeId}&`;
    }

    // Only append keyword if the search box is not empty
    if (this.storeData.search) {
      url += `keyword=${this.storeData.search}&`;
    }

    // Always append pagination params
    url += `page=${backendPage}&size=${this.storeData.pageSize}`;

    // Only add sort/order params when not using the default ascending order
    if (this.storeData.selectedSort !== 'asc') {
      url += `&sort=name&order=${this.storeData.selectedSort}`;
    }

    this.storeService.getProducts(brandId, typeId, url).subscribe({
      next: (data) => {
        this.storeData.products = data.content;              // Current page's product array
        this.storeData.pageable = data.pageable;             // Pagination metadata from Spring
        this.storeData.totalElements = data.totalElements;   // Total matching products (for page count)
        this.storeData.currentPage = data.pageable.pageNumber + 1; // Convert 0-based → 1-based
        this.toastr.success('Products Fetched!!!');
      },
      error: (error) => {
        this.toastr.error('Error fetching data:');
        console.log(error);
      },
    });
  }

  /**
   * getBrands — fetches all brands and prepends the "All" option.
   *
   * The spread operator [...response] preserves the original array while adding
   * {id:0, name:'All'} at position 0 for the "show all brands" filter option.
   */
  getBrands() {
    this.storeService.getBrands().subscribe({
      next: (response) => (this.storeData.brands = [{ id: 0, name: 'All' }, ...response]),
      error: (error) => console.log(error)
    });
  }

  /**
   * getTypes — fetches all product types and prepends the "All" option.
   * Same pattern as getBrands().
   */
  getTypes() {
    this.storeService.getTypes().subscribe({
      next: (response) => (this.storeData.types = [{ id: 0, name: 'All' }, ...response]),
      error: (error) => console.log(error)
    });
  }

  /**
   * selectBrand — updates the selected brand filter and refreshes the product list.
   *
   * @param brand the brand the user clicked in the sidebar ({id:0} = "All")
   */
  selectBrand(brand: Brand) {
    this.storeData.selectedBrand = brand;
    this.fetchProducts(); // Re-fetch with the new brand filter
  }

  /**
   * selectType — updates the selected type filter and refreshes the product list.
   *
   * @param type the type the user clicked in the sidebar ({id:0} = "All")
   */
  selectType(type: Type) {
    this.storeData.selectedType = type;
    this.fetchProducts(); // Re-fetch with the new type filter
  }

  /** onSortChange — called when the sort dropdown changes; re-fetches with new sort order */
  onSortChange() {
    this.fetchProducts();
  }

  /** onSearch — called when the user types in the search box and triggers a search */
  onSearch() {
    this.fetchProducts();
  }

  /** onReset — clears the search term and re-fetches the unfiltered product list */
  onReset() {
    this.storeData.search = ''; // Clear the search term
    this.fetchProducts();
  }
}
