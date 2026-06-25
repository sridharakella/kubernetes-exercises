import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Basket, BasketItem, BasketTotals } from '../shared/models/basket';
import { HttpClient } from '@angular/common/http';
import { Product } from '../shared/models/product';

/**
 * BasketService — manages the shopping basket state and synchronises it with Redis.
 *
 * Architecture:
 *   - Client state:  BehaviorSubject<Basket | null> — all components subscribe to this
 *   - Persistence:   localStorage — survives browser refresh (key: 'basket')
 *   - Server sync:   Redis via Spring Boot /api/baskets (POST on every change)
 *
 * Why BehaviorSubject?
 *   - It holds the current value and immediately emits it to any new subscriber.
 *   - NavBar, BasketComponent, and OrderSummary all subscribe — they all see the
 *     same basket without making additional HTTP calls.
 *
 * Data flow on add-to-basket:
 *   addItemToBasket() → upsertItems() → setBasket() → POST to Redis → BehaviorSubject.next()
 *   → all subscribers (NavBar badge, BasketComponent table) update simultaneously
 */
@Injectable({
  providedIn: 'root'
})
export class BasketService {
  /** Spring Boot basket API base URL */
  apiUrl = 'http://localhost:8080/api/baskets';

  // BehaviorSubject holding the current basket (null = no basket / empty)
  private basketSource = new BehaviorSubject<Basket | null>(null);
  // Public Observable that components subscribe to for basket updates
  basketSource$ = this.basketSource.asObservable();

  // BehaviorSubject holding computed totals (subtotal, shipping, total)
  private basketTotalSource = new BehaviorSubject<BasketTotals>({
    subtotal: 0,
    shipping: 0,
    total: 0
  });
  // Public Observable for totals — used by OrderSummary and CheckoutComponent
  basketTotalSource$ = this.basketTotalSource.asObservable();

  constructor(private http: HttpClient) {
    // On service init (app startup): restore basket from localStorage if it was saved
    // This keeps the cart across browser refreshes without a server round-trip
    const storedBasket = localStorage.getItem('basket');
    if (storedBasket) {
      const parsedBasket = JSON.parse(storedBasket);
      this.basketSource.next(parsedBasket); // Emit saved basket to all subscribers
      this.calculateTotals();               // Recompute subtotal/total for restored basket
    }
  }

  /**
   * updateShippingPrice — updates just the shipping cost in the totals subject.
   *
   * Called by the CheckoutComponent's shipment step when the user selects a
   * delivery option. Recalculates the total without changing other fields.
   *
   * @param shippingPrice the selected delivery cost
   */
  updateShippingPrice(shippingPrice: number): void {
    const updatedBasketTotal = this.basketTotalSource.value; // Read current totals
    updatedBasketTotal.shipping = shippingPrice;
    updatedBasketTotal.total = updatedBasketTotal.subtotal + shippingPrice; // Recalculate total
    this.basketTotalSource.next(updatedBasketTotal); // Emit updated totals
  }

  /**
   * clearBasket — empties the basket in memory and removes it from localStorage.
   *
   * Called after successful checkout to reset the cart to a clean state.
   * Does NOT call the Redis DELETE API — use deleteBasket() for that.
   */
  clearBasket() {
    this.basketSource.next(null);          // Clear in-memory state
    localStorage.removeItem('basket_id');  // Remove basket ID from storage
    localStorage.removeItem('basket');     // Remove basket data from storage
  }

  /**
   * getBasket — fetches the basket from Redis by ID and updates local state.
   *
   * Called on app startup if a basket ID is found in localStorage, ensuring
   * the displayed basket matches what's persisted in Redis.
   *
   * @param id the basket's CUID string from localStorage
   */
  getBasket(id: string) {
    return this.http.get<Basket>(this.apiUrl + '/' + id).subscribe({
      next: basket => {
        this.basketSource.next(basket);              // Update BehaviorSubject
        this.calculateTotals();                       // Recompute totals
        localStorage.setItem('basket', JSON.stringify(basket)); // Sync to localStorage
      }
    });
  }

  /**
   * setBasket — saves the current basket to Redis and updates local state.
   *
   * This is the core write operation — called after every basket mutation
   * (add item, remove item, change quantity). Always sends the FULL basket state,
   * not a diff, because Spring Boot Redis save() is an upsert (full replace).
   *
   * @param basket the complete basket to persist
   */
  setBasket(basket: Basket) {
    return this.http.post<Basket>(this.apiUrl, basket).subscribe({
      next: basket => {
        this.basketSource.next(basket);              // Emit updated basket to subscribers
        this.calculateTotals();                       // Recompute totals after change
        localStorage.setItem('basket', JSON.stringify(basket)); // Keep localStorage in sync
      }
    });
  }

  /**
   * getCurrentBasket — synchronously reads the current basket value.
   *
   * BehaviorSubject.value gives the current value without subscribing.
   * Used inside service methods that need to modify and re-save the basket.
   *
   * @returns the current Basket or null if no basket exists
   */
  getCurrentBasket() {
    return this.basketSource.value;
  }

  /**
   * addItemToBasket — adds a product to the basket (or increments quantity if already there).
   *
   * Flow:
   *   1. Convert Product → BasketItem (mapProductToBasket)
   *   2. Get current basket (or create a new one if null)
   *   3. Upsert the item into the basket's items array
   *   4. Save the updated basket to Redis via setBasket()
   *
   * @param item     the Product to add (from the catalog)
   * @param quantity how many to add (default: 1)
   */
  addItemToBasket(item: Product, quantity = 1) {
    const itemToAdd = this.mapProductToBasket(item);  // Convert Product → BasketItem (snapshot)
    const basket = this.getCurrentBasket() ?? this.createBasket(); // Create basket if null
    basket.items = this.upsertItems(basket.items, itemToAdd, quantity);
    this.setBasket(basket); // Persist and emit
  }

  /**
   * upsertItems — updates item quantity if already in basket, or adds if new.
   *
   * "Upsert" = update + insert. Finds the item by product ID:
   *   - Found → increments the quantity
   *   - Not found → sets quantity and pushes to the array
   *
   * @param items     current basket items array
   * @param itemToAdd the item to add or increment
   * @param quantity  how many to add
   * @returns updated items array
   */
  upsertItems(items: BasketItem[], itemToAdd: BasketItem, quantity: number): BasketItem[] {
    const item = items.find(x => x.id === itemToAdd.id); // Look for existing item
    if (item) {
      item.quantity += quantity; // Increment if found
    } else {
      itemToAdd.quantity = quantity; // Set quantity and add if new
      items.push(itemToAdd);
    }
    return items;
  }

  /**
   * createBasket — creates a new empty basket with a CUID and saves its ID.
   *
   * The Basket class constructor auto-generates a CUID using @paralleldrive/cuid2.
   * The basket ID is saved to localStorage so it can be used to fetch/restore
   * the basket on next page load.
   *
   * @returns a new empty Basket instance
   */
  createBasket(): Basket {
    const basket = new Basket(); // CUID is generated here by the Basket class constructor
    localStorage.setItem('basket_id', basket.id); // Save ID for future session restoration
    return basket;
  }

  /**
   * incrementItemQuantity — increases a basket item's quantity by the given amount.
   *
   * Includes a guard to prevent quantity dropping below 1 (handles edge cases
   * where increment is called with a negative value).
   *
   * @param itemId   the product ID of the item to increment
   * @param quantity amount to add (default: 1)
   */
  incrementItemQuantity(itemId: number, quantity: number = 1) {
    const basket = this.getCurrentBasket();
    if (basket) {
      const item = basket.items.find((p) => p.id === itemId);
      if (item) {
        item.quantity += quantity;
        if (item.quantity < 1) {
          item.quantity = 1; // Floor at 1 — prevent negative quantity
        }
        this.setBasket(basket);
      }
    }
  }

  /**
   * decrementItemQuantity — decreases a basket item's quantity by the given amount.
   *
   * Only decrements if the result would remain > 1 (to remove entirely, use remove()).
   * This prevents accidental zero-quantity items.
   *
   * @param itemId   the product ID of the item to decrement
   * @param quantity amount to subtract (default: 1)
   */
  decrementItemQuantity(itemId: number, quantity: number = 1) {
    const basket = this.getCurrentBasket();
    if (basket) {
      const item = basket.items.find((p) => p.id === itemId);
      if (item && item.quantity > 1) { // Only decrement if result stays positive
        item.quantity -= quantity;
        this.setBasket(basket);
      }
    }
  }

  /**
   * remove — removes an item entirely from the basket.
   *
   * Uses findIndex + splice to remove the item by position.
   * If the basket becomes empty after removal, clears the basket state and localStorage
   * (avoids sending an empty basket to Redis).
   *
   * @param itemId the product ID of the item to remove
   */
  remove(itemId: number) {
    const basket = this.getCurrentBasket();
    if (basket) {
      const itemIndex = basket.items.findIndex((p) => p.id === itemId);
      if (itemIndex !== -1) {
        basket.items.splice(itemIndex, 1); // Remove item at found index
        this.setBasket(basket);            // Save updated basket to Redis
      }
      // If basket is now empty, clear state and localStorage
      if (basket.items.length === 0) {
        this.basketSource.next(null);
        localStorage.removeItem('basket_id');
        localStorage.removeItem('basket');
      }
    }
  }

  /**
   * calculateTotals — recomputes shipping, subtotal, and total from the current basket.
   *
   * Called after every basket change (add, remove, quantity update) and on basket restore.
   * Uses Array.reduce to sum (price × quantity) for all items.
   * Shipping is hardcoded to 0 for now — updateShippingPrice() applies delivery charges
   * in the checkout flow.
   */
  private calculateTotals() {
    const basket = this.getCurrentBasket();
    if (!basket) return;
    const shipping = 0; // Free shipping by default; updated in checkout
    // Reduce: accumulate (price × quantity) for every item
    const subTotal = basket.items.reduce((x, y) => (y.price * y.quantity) + x, 0);
    const total = subTotal + shipping;
    this.basketTotalSource.next({ shipping, subtotal: subTotal, total });
  }

  /**
   * mapProductToBasket — converts a Product catalog item to a BasketItem.
   *
   * Denormalises the product at the moment it's added: copies name, price, and brand/type
   * as plain strings into the basket item. This snapshot ensures the basket still reflects
   * what the user saw even if the catalog changes later (pricing integrity).
   *
   * @param item the Product from the catalog
   * @returns a BasketItem with snapshotted product data (quantity initialised to 0)
   */
  private mapProductToBasket(item: Product): BasketItem {
    return {
      id: item.id,
      name: item.name,
      price: item.price,           // Snapshot price at time of add
      description: item.description,
      quantity: 0,                 // Set to 0 here; upsertItems sets the real quantity
      pictureUrl: item.pictureUrl,
      productBrand: item.productBrand, // Denormalised string — no FK lookup needed
      productType: item.productType    // Denormalised string — no FK lookup needed
    };
  }
}
