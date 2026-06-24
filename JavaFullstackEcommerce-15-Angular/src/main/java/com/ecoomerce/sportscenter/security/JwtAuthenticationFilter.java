package com.ecoomerce.sportscenter.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter — Spring Security filter that validates JWT tokens on every HTTP request.
 *
 * This filter intercepts every incoming HTTP request and:
 *   1. Reads the "Authorization" header looking for a Bearer token
 *   2. Extracts the username from the JWT
 *   3. Loads the full UserDetails from UserDetailsService
 *   4. Validates the token (correct signature, not expired, username matches)
 *   5. If valid, sets the authentication in the SecurityContext so Spring Security
 *      treats this request as authenticated
 *
 * extends OncePerRequestFilter — guarantees this filter runs exactly ONCE per HTTP request,
 * even in complex filter chains (prevents double-execution in forwarded requests).
 *
 * This filter is registered in SecurityConfig before UsernamePasswordAuthenticationFilter
 * so JWT auth is evaluated before Spring's default form-based auth.
 *
 * @Component : makes this a Spring-managed singleton bean
 * @Log4j2    : injects a Log4j2 logger (via Lombok) for structured logging
 */
@Component
@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtHelper jwtHelper;              // Utility for JWT parsing and validation
    private final UserDetailsService userDetailsService; // Loads user details by username

    /**
     * Constructor injection — Spring injects both dependencies.
     *
     * @param jwtHelper          JWT utility (parse, validate, extract claims)
     * @param userDetailsService loads UserDetails from the in-memory user store (MyConfig)
     */
    public JwtAuthenticationFilter(JwtHelper jwtHelper, UserDetailsService userDetailsService) {
        this.jwtHelper = jwtHelper;
        this.userDetailsService = userDetailsService;
    }

    /**
     * doFilterInternal — core JWT validation logic executed on every HTTP request.
     *
     * Flow:
     *   1. Read the "Authorization" header from the request
     *   2. If it starts with "Bearer ", strip the prefix to get the raw token
     *   3. Extract the username from the token using JwtHelper
     *   4. If username extracted AND SecurityContext has no current authentication:
     *        a. Load UserDetails by username
     *        b. Validate the token against those UserDetails
     *        c. If valid, build a UsernamePasswordAuthenticationToken and set it
     *           in the SecurityContext — this marks the request as authenticated
     *   5. Always call filterChain.doFilter() to pass the request to the next filter/controller
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response (modified only if filter logic needs to write a response)
     * @param filterChain the chain of remaining filters — must be called to continue processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestHeader = request.getHeader("Authorization");  // Read the Authorization header
        log.info("Header: {}", requestHeader);
        String userName = null;     // Extracted username from JWT (null if no valid token)
        String token = null;        // Raw JWT string (without "Bearer " prefix)

        if(requestHeader != null && requestHeader.startsWith("Bearer")){
            // Strip the "Bearer " prefix (7 characters) to get the raw JWT token string
            token = requestHeader.substring(7);
            try{
                // Extract the username (subject claim) from the JWT payload
                userName = this.jwtHelper.getUserNameFromToken(token);
            } catch(IllegalArgumentException e){
                // Token string is null or empty
                log.info("Jwt Token processing error");
                e.printStackTrace();
            } catch(ExpiredJwtException e){
                // Token's expiration date is in the past — user must log in again
                log.info("Jwt Token processing error");
                e.printStackTrace();
            } catch(MalformedJwtException e){
                // Token structure is invalid — likely tampered with or corrupt
                log.info("Jwt Token processing error");
                e.printStackTrace();
            }
        } else {
            // No Authorization header or it doesn't start with "Bearer" — anonymous request
            log.warn("JWT token doesn't begin with Bearer String");
        }

        // Only proceed with authentication if:
        //   - We successfully extracted a username from the token
        //   - The SecurityContext doesn't already have an authenticated user
        //     (avoids re-authenticating on every filter in the chain)
        if(userName != null && SecurityContextHolder.getContext().getAuthentication() == null){
            // Load full UserDetails (username, password hash, roles) from the user store
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userName);

            // Validate the token — checks username match and expiry
            Boolean validateToken = this.jwtHelper.validateToken(token, userDetails);

            if(validateToken){
                // Token is valid — build an authenticated token and set it in the SecurityContext
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,              // Principal (the authenticated user)
                                null,                     // Credentials (null — we don't need the password after auth)
                                userDetails.getAuthorities()  // Granted authorities/roles
                        );
                // Attach request metadata (IP address, session ID) to the authentication token
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Set the authentication in the SecurityContext — this request is now authenticated
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else{
                log.info("Validation fails"); // Token exists but is invalid or expired
            }
        }
        // Continue the filter chain — pass the request to the next filter or controller
        filterChain.doFilter(request, response);
    }
}
