package com.ecoomerce.sportscenter.exceptions;

/**
 * ProductNotFoundException — custom unchecked exception for missing products.
 *
 * Extends RuntimeException (unchecked) so callers don't need to declare it in
 * method signatures or catch it explicitly. Spring's @ExceptionHandler in
 * CustomExceptionHandler catches it and returns a 404 Not Found response.
 *
 * Usage:
 *   productRepository.findById(id)
 *       .orElseThrow(() -> new ProductNotFoundException("Product with given id doesn't exist"));
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive error message.
     *
     * @param message human-readable explanation of why the product was not found
     *                (included in the CustomErrorResponse body returned to the client)
     */
    public ProductNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a message and a root cause.
     * Use this variant when wrapping a lower-level exception
     * (e.g. a database access error that results in a product not being found).
     *
     * @param message human-readable explanation
     * @param cause   the underlying exception that caused this one
     */
    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
