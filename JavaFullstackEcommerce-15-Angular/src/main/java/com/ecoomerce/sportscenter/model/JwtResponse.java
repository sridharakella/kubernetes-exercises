package com.ecoomerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JwtResponse — DTO returned to the Angular client after a successful login.
 *
 * Sent as the response body of POST /auth/login on success (HTTP 200 OK).
 *
 * Example JSON response received by Angular:
 * {
 *   "username": "rahul",
 *   "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJyYWh1bCIsImlhdCI..."
 * }
 *
 * The Angular AccountService:
 *   1. Receives this response
 *   2. Stores the token in localStorage under the key "token"
 *   3. Stores the username in the currentUserSource BehaviorSubject
 *   4. Attaches the token as "Authorization: Bearer <token>" on all subsequent HTTP requests
 *
 * The token is valid for 5 hours (defined in JwtHelper.JWT_TOKEN_VALIDITY).
 * After expiry, the Angular ErrorInterceptor catches the 401 response and
 * redirects the user to log in again.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {
    private String username; // The authenticated user's username (for display in the Angular navbar)
    private String token;    // The signed JWT — sent with every subsequent API request
}
