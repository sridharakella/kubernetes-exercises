/**
 * User interface — the authentication payload returned by Spring Boot's /auth/login endpoint.
 *
 * Maps to JwtResponse on the backend (the auth DTO, not a UserDetails entity).
 * AccountService stores this in memory via BehaviorSubject and persists the token to localStorage.
 *
 * Fields:
 *   username — the authenticated user's username; displayed in the NavBar
 *   token    — the signed JWT (HS512); sent as "Authorization: Bearer <token>"
 *              on authenticated API calls (e.g. /auth/user, checkout endpoints)
 *
 * Security note: the token is stored in localStorage, which is accessible to JavaScript.
 * For higher security, consider HttpOnly cookies (XSS-resistant) in production.
 */
export interface User {
  username: string; // Display name shown in the NavBar when logged in
  token: string;    // JWT — store in localStorage; include in Authorization header for auth requests
}
