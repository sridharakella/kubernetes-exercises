package com.ecoomerce.sportscenter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig — Cross-Origin Resource Sharing (CORS) configuration.
 *
 * CORS is a browser security mechanism that blocks a web page from making HTTP requests
 * to a different origin (domain + port) than the one that served the page.
 *
 * Problem this solves:
 *   The Angular frontend runs on http://localhost:4200 (Angular dev server).
 *   The Spring Boot API runs on http://localhost:8080.
 *   Without CORS configuration, the browser would block all API calls from the frontend.
 *
 * @Configuration  : registers this as a Spring configuration class
 * @EnableWebMvc   : enables Spring MVC and allows this class to override WebMvcConfigurer
 *                   methods to customise MVC behaviour (here, CORS mappings)
 *
 * NOTE: In production, replace allowedOrigins("*") with the actual frontend domain
 *       (e.g. "https://yoursite.com") to restrict cross-origin access.
 */
@Configuration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer {

    /**
     * addCorsMappings — registers CORS rules for all API routes.
     *
     * "/*/**" : matches all paths with at least one path segment (e.g. /api/products, /auth/login)
     * allowedOrigins("*") : allows requests from ANY origin (any domain or port)
     *                       — suitable for development, restrict in production
     * allowedMethods       : only GET, POST, PUT, DELETE HTTP methods are allowed cross-origin
     * allowedHeaders("*")  : any request header is permitted (needed for Authorization: Bearer <token>)
     *
     * @param registry the CorsRegistry builder provided by Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/*/**")          // Apply CORS to all sub-paths
                .allowedOrigins("*")           // Allow any origin (Angular dev server, Postman, etc.)
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Permitted HTTP methods
                .allowedHeaders("*");          // Allow all headers (including Authorization for JWT)
    }
}
