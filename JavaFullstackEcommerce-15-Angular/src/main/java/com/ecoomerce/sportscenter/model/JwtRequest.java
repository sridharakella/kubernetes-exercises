package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JwtRequest — DTO carrying login credentials from the Angular client to the login endpoint.
 *
 * Received as the request body of POST /auth/login.
 *
 * Example JSON body sent by Angular:
 * {
 *   "username": "rahul",
 *   "password": "Password"
 * }
 *
 * IMPORTANT: Never log the password field — it is plain text in transit
 * (protected only by HTTPS in production). Spring Security's AuthenticationManager
 * compares this against the BCrypt-hashed password stored in the user store.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtRequest {
    private String username; // The user's login name (e.g. "rahul")
    private String password; // The plain-text password — NEVER log this field
}
