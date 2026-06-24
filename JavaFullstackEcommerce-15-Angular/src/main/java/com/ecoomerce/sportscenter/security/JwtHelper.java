package com.ecoomerce.sportscenter.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtHelper — utility component for JWT (JSON Web Token) operations.
 *
 * Responsibilities:
 *   1. Generate a JWT when a user successfully logs in
 *   2. Parse a JWT to extract the username (subject) from the token payload
 *   3. Validate a JWT (correct username + not expired)
 *
 * JWT Structure:  header.payload.signature
 *   - Header    : algorithm (HS512) and token type (JWT)
 *   - Payload   : claims — subject (username), issued-at, expiration
 *   - Signature : HMAC-SHA512 signature using the secret key (verifies authenticity)
 *
 * IMPORTANT: The secret key is hardcoded here for simplicity.
 * In production, load it from environment variables or a secrets manager (e.g. AWS Secrets Manager).
 *
 * @Component : registers this class as a Spring bean, injectable into other components
 */
@Component
public class JwtHelper {

    /**
     * Token validity duration in seconds.
     * 5 * 60 * 60 = 18,000 seconds = 5 hours.
     * After this period, the token expires and the user must log in again.
     */
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

    /**
     * Secret key for signing and verifying JWTs.
     * Must be kept private — anyone with this key can forge valid tokens.
     * NOTE: In production, store this in environment variables or a vault,
     *       never hardcoded in source code.
     */
    private String secret = "f27dacd186810e78c0fd8ba65ecf3f1524ff087c5e86773d5172d424b3fd201f";

    /**
     * getUserNameFromToken — extracts the username (subject claim) from a JWT.
     *
     * The subject is set to the username when the token is generated.
     * Called by JwtAuthenticationFilter to identify the authenticated user.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return the username stored in the token's subject claim
     */
    public String getUserNameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * getExpirationDateFromToken — extracts the token's expiration date.
     *
     * Used by isTokenExpired() to determine if the token is still valid.
     *
     * @param token the raw JWT string
     * @return the expiration Date encoded in the token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * getClaimFromToken — generic method to extract any claim from the JWT payload.
     *
     * Uses a Function<Claims, T> so callers can specify which claim to extract
     * (e.g. Claims::getSubject for username, Claims::getExpiration for expiry date).
     *
     * @param token          the raw JWT string
     * @param claimsResolver a function that maps the Claims object to the desired value
     * @param <T>            the type of the extracted claim
     * @return the resolved claim value
     */
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);  // Parse all claims from the token
        return claimsResolver.apply(claims);                 // Apply the extractor function
    }

    /**
     * getAllClaimsFromToken — parses the JWT and returns all claims from the payload.
     *
     * Steps:
     *   1. Convert the secret string to a Key using HMAC-SHA256
     *   2. Use the JJWT parser to verify the signature and parse the token body
     *   3. Return the Claims object containing all key-value pairs from the payload
     *
     * Throws exceptions (handled by JwtAuthenticationFilter) if:
     *   - The token is expired (ExpiredJwtException)
     *   - The token signature is invalid (SignatureException)
     *   - The token format is wrong (MalformedJwtException)
     *
     * @param token the raw JWT string
     * @return Claims object containing all token payload entries
     */
    private Claims getAllClaimsFromToken(String token) {
        // Build the HMAC-SHA256 key from the secret string bytes
        Key hmacKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return Jwts.parserBuilder()
                .setSigningKey(hmacKey)     // Use the same key that was used to sign the token
                .build()
                .parseClaimsJws(token)      // Parse and verify the signed JWT
                .getBody();                 // Return the claims payload
    }

    /**
     * isTokenExpired — checks if the token's expiration date is in the past.
     *
     * @param token the raw JWT string
     * @return true if the token has expired; false if it is still valid
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());  // Compare expiry date to current time
    }

    /**
     * generateToken — generates a JWT for an authenticated user.
     *
     * Called by AuthConroller after successful username/password authentication.
     * The token encodes the username as the subject and sets issued-at and expiration times.
     *
     * @param userDetails the authenticated user's details (from UserDetailsService)
     * @return a signed JWT string to be returned to the client
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();  // Additional claims (empty here; can add roles, etc.)
        return generateToken(claims, userDetails.getUsername());
    }

    /**
     * generateToken (private overload) — builds and signs the JWT.
     *
     * Token contents:
     *   - claims       : additional payload key-value pairs (empty in this implementation)
     *   - subject      : the username (used to identify the user on subsequent requests)
     *   - issuedAt     : current timestamp
     *   - expiration   : current time + JWT_TOKEN_VALIDITY seconds (5 hours)
     *   - signWith     : HMAC-SHA512 using the secret key (stronger than HS256 for production use)
     *
     * @param claims  additional claims to include in the token payload
     * @param subject the username to set as the token subject
     * @return compact, signed JWT string (base64url encoded)
     */
    private String generateToken(Map<String, Object> claims, String subject) {
        // Convert the secret string key into a Key object for HS512 signing
        Key hmacKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS512.getJcaName());

        return Jwts.builder()
                .setClaims(claims)                                                  // Set additional claims
                .setSubject(subject)                                                // Set username as subject
                .setIssuedAt(new Date(System.currentTimeMillis()))                 // Token creation time
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000)) // Expiry: now + 5h
                .signWith(hmacKey, SignatureAlgorithm.HS512)                       // Sign with HMAC-SHA512
                .compact();                                                         // Serialise to compact JWT string
    }

    /**
     * validateToken — checks if a JWT is valid for the given user.
     *
     * Validation criteria:
     *   1. The username in the token matches the loaded UserDetails username
     *   2. The token has not expired
     *
     * Called by JwtAuthenticationFilter after extracting the username from the token
     * and loading the user from UserDetailsService.
     *
     * @param token       the raw JWT string
     * @param userDetails the user details loaded from UserDetailsService
     * @return true if the token is valid for this user; false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUserNameFromToken(token);
        // Token is valid if: username matches AND token is not expired
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

}
