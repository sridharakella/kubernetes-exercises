package com.ecoomerce.sportscenter.controller;

import com.ecoomerce.sportscenter.model.JwtRequest;
import com.ecoomerce.sportscenter.model.JwtResponse;
import com.ecoomerce.sportscenter.security.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * AuthConroller — REST controller for authentication endpoints.
 * (Note: class name has a typo — should be "AuthController")
 *
 * Exposes two endpoints:
 *   POST /auth/login  — authenticates a user and returns a JWT
 *   GET  /auth/user   — validates a JWT and returns the authenticated user's details
 *
 * Security note: /auth/login is open (permitAll) in SecurityConfig.
 * /auth/user is also open but requires a valid Bearer token to return useful data.
 *
 * @RestController : combines @Controller + @ResponseBody — all methods return JSON by default
 * @RequestMapping : all routes in this controller are prefixed with "/auth"
 */
@RestController
@RequestMapping("/auth")
public class AuthConroller {

    private final UserDetailsService userDetailsService; // Loads user details by username
    private final AuthenticationManager manager;         // Spring Security auth manager (validates credentials)
    private final JwtHelper jwtHelper;                   // JWT generator and parser

    /**
     * Constructor injection — Spring wires all three dependencies.
     *
     * @param userDetailsService loads UserDetails from the in-memory store (MyConfig)
     * @param manager            performs username/password authentication
     * @param jwtHelper          generates and parses JWT tokens
     */
    public AuthConroller(UserDetailsService userDetailsService, AuthenticationManager manager, JwtHelper jwtHelper) {
        this.userDetailsService = userDetailsService;
        this.manager = manager;
        this.jwtHelper = jwtHelper;
    }

    /**
     * login — POST /auth/login
     *
     * Authenticates the user with username/password and returns a JWT on success.
     *
     * Request body: { "username": "rahul", "password": "Password" }
     * Response:     { "username": "rahul", "token": "<JWT string>" }
     *
     * Flow:
     *   1. Call authenticate() to verify credentials via the AuthenticationManager
     *      (throws BadCredentialsException if credentials are wrong)
     *   2. Load the full UserDetails object for the authenticated username
     *   3. Generate a JWT using JwtHelper (includes username as subject, valid for 5 hours)
     *   4. Return the JWT and username as a JwtResponse
     *
     * @param request the login request body containing username and password
     * @return 200 OK with the JWT on success; 401 if credentials are invalid (via exception)
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody JwtRequest request){
        this.authenticate(request.getUsername(), request.getPassword()); // Step 1: verify credentials
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername()); // Step 2: load user
        String token = this.jwtHelper.generateToken(userDetails); // Step 3: generate JWT
        JwtResponse response = JwtResponse.builder()
                .username(userDetails.getUsername())
                .token(token)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK); // Step 4: return JWT
    }

    /**
     * getUserDetails — GET /auth/user
     *
     * Extracts the username from the JWT in the Authorization header and returns
     * the corresponding UserDetails. Used by the Angular frontend to restore the
     * current user's session after a page refresh (reads token from localStorage).
     *
     * Authorization header format: "Bearer <JWT>"
     *
     * @param tokenHeader the raw Authorization header value from the HTTP request
     * @return 200 OK with UserDetails if token is valid; 400 Bad Request if header is missing/malformed
     */
    @GetMapping("/user")
    public ResponseEntity<UserDetails> getUserDetails(@RequestHeader("Authorization") String tokenHeader) {
        String token = extractTokenFromHeader(tokenHeader); // Strip "Bearer " prefix
        if (token != null) {
            String username = jwtHelper.getUserNameFromToken(token); // Extract username from JWT payload
            UserDetails userDetails = userDetailsService.loadUserByUsername(username); // Load full user details
            return new ResponseEntity<>(userDetails, HttpStatus.OK);
        } else {
            // Token header was missing or malformed — return 400 Bad Request
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * extractTokenFromHeader — strips the "Bearer " prefix from the Authorization header value.
     *
     * JWT tokens are sent as "Bearer eyJhbG..." — we only need the token part after the prefix.
     *
     * @param tokenHeader the raw Authorization header value (e.g. "Bearer eyJ...")
     * @return the raw JWT string (without "Bearer " prefix), or null if the header is invalid
     */
    private String extractTokenFromHeader(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            return tokenHeader.substring(7); // Remove "Bearer " prefix (7 characters)
        }
        return null; // Invalid or missing header
    }

    /**
     * authenticate — delegates username/password verification to the AuthenticationManager.
     *
     * The AuthenticationManager uses the UserDetailsService (MyConfig) and PasswordEncoder (BCrypt)
     * to verify the credentials. If they are invalid, it throws BadCredentialsException.
     *
     * @param username the username from the login request
     * @param password the plain-text password from the login request
     * @throws BadCredentialsException if authentication fails (username not found or wrong password)
     */
    private void authenticate(String username, String password) {
        // Wrap credentials in a Spring Security authentication token (pre-auth state)
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        try{
            // Ask the AuthenticationManager to verify: loads UserDetails + BCrypt-compares passwords
            manager.authenticate(authenticationToken);
        } catch(BadCredentialsException e){
            // Re-throw with a clear message — propagates up to the caller (login endpoint)
            throw new BadCredentialsException("Invalid Username or Password");
        }
    }
}
