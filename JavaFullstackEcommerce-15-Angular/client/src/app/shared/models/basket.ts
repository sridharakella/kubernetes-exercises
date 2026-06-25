import { createId } from "@paralleldrive/cuid2"

/**
 * Basket interface — defines the shape of a basket object as received from/sent to the API.
 *
 * The basket ID is a CUID string generated client-side (not by the server).
 * This is intentional: Angular owns basket identity, enabling offline-first basket creation
 * without needing a server round-trip just to create an empty basket.
 */
export interface Basket {
  id: string          // CUID (e.g. "clh12abc...") — used as the Redis key suffix
  items: BasketItem[] // All items currently in the basket
}

/**
 * BasketItem interface — a single product in the basket with a quantity and snapshotted details.
 *
 * All product fields are denormalised (copied from the Product at add-time):
 *   - price is snapshotted to preserve pricing integrity — if the catalog price changes,
 *     the basket still shows what the user saw when they added the item.
 *   - productBrand and productType are stored as strings (not IDs) — the basket UI
 *     can display them without any additional API lookups.
 */
export interface BasketItem {
  id: number           // Product ID — used to find and update this item in the basket
  name: string         // Product name snapshot
  description: string  // Product description snapshot
  price: number        // Price snapshot at time of add (smallest currency unit)
  pictureUrl: string   // Product image URL snapshot
  productBrand: string // Brand name snapshot (denormalised — no FK needed)
  productType: string  // Type/category name snapshot (denormalised — no FK needed)
  quantity: number     // Number of this product in the basket
}

/**
 * Basket class — the concrete implementation used to CREATE a new basket.
 *
 * Implements the Basket interface. Uses a class (not just an interface) because:
 *   - It needs an initialiser: id = createId() auto-generates a CUID on instantiation
 *   - BasketService calls `new Basket()` to create empty baskets
 *
 * createId() from @paralleldrive/cuid2 generates a collision-resistant unique string ID
 * (e.g. "clh12abc..."). No server call needed — the client assigns the basket ID itself.
 */
export class Basket implements Basket {
  id = createId(); // Generate a unique CUID for this basket when it's created
  items: BasketItem[] = []; // Start with an empty items array
}

/**
 * BasketTotals interface — computed totals shown in the order summary.
 *
 * Calculated by BasketService.calculateTotals() from the current basket items.
 * Emitted via basketTotalSource$ BehaviorSubject to update the order summary display.
 */
export interface BasketTotals {
  shipping: number; // Delivery cost (0 until user selects a delivery option in checkout)
  subtotal: number; // Sum of (price × quantity) for all items
  total: number;    // subtotal + shipping
}
