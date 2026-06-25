/**
 * Product interface — the shape of a single product from the Spring Boot API.
 *
 * Maps to ProductResponse on the backend (the DTO, not the JPA entity directly).
 * The backend converts the Product entity → ProductResponse to:
 *   - Flatten the @ManyToOne Brand and Type associations into plain strings
 *   - Prevent Jackson infinite serialisation from bidirectional entity relationships
 *
 * Fields:
 *   id          — JPA primary key; used to find this product in the basket
 *   name        — display name shown in product cards and details page
 *   description — full description shown on the product detail page
 *   price       — price in smallest currency unit (e.g. pence/cents); divide by 100 for display
 *   pictureUrl  — relative or absolute URL to the product image
 *   productType — category name as a plain string (e.g. "Tennis", "Football")
 *   productBrand — brand name as a plain string (e.g. "Nike", "Adidas")
 */
export interface Product {
  id: number
  name: string
  description: string
  price: number
  pictureUrl: string
  productType: string   // Denormalised string from Spring ProductResponse (not a FK object)
  productBrand: string  // Denormalised string from Spring ProductResponse (not a FK object)
}
