package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * CustomErrorResponse — structured error payload returned by the API on exceptions.
 *
 * Produced by CustomExceptionHandler and returned in the HTTP response body when
 * an exception is caught globally. Replaces Spring Boot's default Whitelabel Error Page
 * with a consistent JSON error format that Angular can parse and display.
 *
 * Example JSON response for a 404:
 * {
 *   "status": "NOT_FOUND",
 *   "error": "Product not found",
 *   "message": "Product with given id doesn't exist"
 * }
 *
 * Angular's ErrorInterceptor receives this and displays the error via Toastr notifications.
 *
 * Note: @Builder is not used here because CustomExceptionHandler constructs this
 * via the @AllArgsConstructor (all three args at once). Add @Builder if more
 * exception types with different field combinations are added in future.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomErrorResponse {
    private HttpStatus status;  // HTTP status enum (e.g. NOT_FOUND, BAD_REQUEST) — serialised as a string
    private String error;       // High-level, user-facing error label (e.g. "Product not found")
    private String message;     // Detailed error message from the exception (e.g. "Product with given id doesn't exist")
}
