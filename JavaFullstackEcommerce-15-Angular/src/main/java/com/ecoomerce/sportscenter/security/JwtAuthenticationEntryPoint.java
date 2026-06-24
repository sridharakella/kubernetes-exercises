package com.ecoomerce.sportscenter.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * JwtAuthenticationEntryPoint — handles unauthenticated access to protected resources.
 *
 * By default, Spring Security redirects unauthenticated requests to a login page.
 * This entry point overrides that behaviour and instead returns a 401 Unauthorized
 * HTTP response with a plain-text error message — appropriate for a REST API consumed
 * by an Angular frontend (not a browser-rendered page).
 *
 * Registered in SecurityConfig via:
 *   .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
 *
 * When is this triggered?
 *   - A request hits an endpoint that requires authentication (e.g. "/products")
 *   - No valid JWT is present in the Authorization header
 *   - JwtAuthenticationFilter did not set an authentication in the SecurityContext
 *   → Spring Security calls commence() on this entry point instead of proceeding
 *
 * @Component : registers this as a Spring bean, injectable into SecurityConfig
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * commence — sends a 401 Unauthorized response for unauthenticated requests.
     *
     * @param request         the HTTP request that triggered the authentication failure
     * @param response        the HTTP response to write the 401 status and message to
     * @param authException   the exception that caused the authentication failure
     *                        (contains a message explaining why auth failed)
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Write HTTP 401 status code

        // Write the error message as plain text to the response body
        // The Angular error interceptor will receive this and can display it to the user
        PrintWriter writer = response.getWriter();
        writer.println("Access Denied: " + authException.getMessage());
    }
}
