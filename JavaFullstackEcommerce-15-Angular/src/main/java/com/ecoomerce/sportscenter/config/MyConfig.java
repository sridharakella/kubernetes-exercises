package com.ecoomerce.sportscenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * MyConfig — Spring Security user store and password encoder configuration.
 *
 * This class provides two important Spring Security beans:
 *   1. UserDetailsService — tells Spring Security how to look up a user by username
 *   2. PasswordEncoder    — tells Spring Security how to hash and verify passwords
 *
 * NOTE: This implementation uses an in-memory user store with a single hardcoded user.
 *       In a production application, replace InMemoryUserDetailsManager with a
 *       JPA-backed UserDetailsService that loads users from a database.
 *
 * @Configuration : marks this class as a Spring configuration class so its @Bean
 *                  methods are registered in the application context
 */
@Configuration
public class MyConfig {

    /**
     * userDetailsService — defines the user store used for authentication.
     *
     * Creates one in-memory user:
     *   username: "rahul"
     *   password: "Password" (BCrypt-hashed; never stored in plain text)
     *   roles:    "admin"
     *
     * InMemoryUserDetailsManager stores the user in memory (no database).
     * Spring Security calls this service when:
     *   - AuthController authenticates a login request
     *   - JwtAuthenticationFilter loads user details to validate a JWT
     *
     * @return UserDetailsService bean used by Spring Security
     */
    @Bean
    public UserDetailsService userDetailsService(){
        UserDetails userDetails = User.builder()
                .username("rahul")                          // Login username
                .password(passwordEncoder().encode("Password"))  // Password encoded with BCrypt
                .roles("admin")                             // Granted role (prefixed as ROLE_admin internally)
                .build();
        return new InMemoryUserDetailsManager(userDetails); // Single-user in-memory store
    }

    /**
     * passwordEncoder — defines the BCrypt password hashing strategy.
     *
     * BCryptPasswordEncoder:
     *   - Applies a slow, salted hash algorithm — resistant to brute-force attacks
     *   - The same plain text password produces a different hash each time (due to random salt)
     *   - Spring Security uses this to hash new passwords and to verify login passwords
     *
     * This bean is also used in MyConfig.userDetailsService() to hash the initial user's password.
     *
     * @return PasswordEncoder bean registered in the Spring context
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();  // Industry-standard adaptive hashing for passwords
    }
}
