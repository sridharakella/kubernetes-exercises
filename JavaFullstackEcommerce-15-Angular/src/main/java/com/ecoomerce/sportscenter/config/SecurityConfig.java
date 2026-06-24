package com.ecoomerce.sportscenter.config;

import com.ecoomerce.sportscenter.security.JwtAuthenticationEntryPoint;
import com.ecoomerce.sportscenter.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — Spring Security configuration for the SportsCenter API.
 *
 * This class defines:
 *   1. Which endpoints are publicly accessible vs. require authentication
 *   2. Stateless session management (JWT-based — no server-side HTTP sessions)
 *   3. The custom JWT filter that intercepts every request before Spring's default auth filter
 *   4. The custom entry point that returns a 401 response for unauthenticated access
 *
 * @Configuration    : registers this class as a Spring configuration bean
 * @EnableMethodSecurity : enables @PreAuthorize / @PostAuthorize annotations on individual
 *                        controller methods for fine-grained method-level security
 */
@Configuration
@EnableMethodSecurity()
public class SecurityConfig {

    // Custom entry point that writes a 401 Unauthorized response when authentication fails
    private final JwtAuthenticationEntryPoint entryPoint;

    // Custom filter that reads the JWT from the Authorization header and authenticates the request
    private final JwtAuthenticationFilter filter;

    @Autowired
    private AuthenticationManagerBuilder authenticationManagerBuilder;  // Used to build the AuthenticationManager bean

    /**
     * Constructor injection — Spring injects both security components when building this bean.
     *
     * @param entryPoint handles unauthenticated requests (returns 401)
     * @param filter     intercepts each request to extract and validate the JWT token
     */
    public SecurityConfig(JwtAuthenticationEntryPoint entryPoint, JwtAuthenticationFilter filter) {
        this.entryPoint = entryPoint;
        this.filter = filter;
    }

    /**
     * SecurityFilterChain — the main security policy for HTTP requests.
     *
     * Configuration decisions:
     *   - CSRF disabled: JWT is used for auth (stateless), so CSRF protection is not needed
     *   - /products requires authentication (authenticated() rule)
     *   - /auth/login is open to all (permitAll) — this is the login endpoint
     *   - anyRequest().permitAll() — all other routes (e.g. /api/products) are open by default
     *     (individual methods use @PreAuthorize for finer control if needed)
     *   - exceptionHandling: uses custom entryPoint to return 401 instead of Spring's default redirect
     *   - SessionCreationPolicy.STATELESS: Spring will NOT create or use HTTP sessions;
     *     authentication state is carried entirely in the JWT on each request
     *   - addFilterBefore: our JwtAuthenticationFilter runs before UsernamePasswordAuthenticationFilter
     *     so the JWT is validated and the SecurityContext is set up before Spring's default auth logic
     *
     * @param http the HttpSecurity builder provided by Spring
     * @return the built SecurityFilterChain bean
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)          // Disable CSRF (not needed with JWT)
                .authorizeHttpRequests((requests)-> requests
                        .requestMatchers("/products").authenticated()     // /products requires a valid JWT
                        .requestMatchers("/auth/login").permitAll()       // Login endpoint is open
                        .anyRequest().permitAll())                        // All other routes open by default
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))  // Use custom 401 handler
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // No HTTP sessions
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class); // JWT filter runs first
        return http.build();

    }

    /**
     * AuthenticationManager bean — required by AuthConroller to authenticate login requests.
     * Delegates to the AuthenticationManagerBuilder which uses the UserDetailsService
     * and PasswordEncoder defined in MyConfig.
     *
     * @param http the HttpSecurity instance (not used directly; needed to satisfy Spring's bean lifecycle)
     * @return the configured AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return authenticationManagerBuilder.getObject();
    }
}
