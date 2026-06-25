package com.ecoomerce.sportscenter.exceptions;

import com.ecoomerce.sportscenter.model.CustomErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * CustomExceptionHandler — global exception handler for all REST controllers.
 *
 * @ControllerAdvice makes this class intercept exceptions thrown from ANY
 * @RestController or @Controller in the application. Without it, exceptions
 * would propagate as unhandled 500 Internal Server Error responses.
 *
 * Benefits of centralised exception handling:
 *   - No try-catch blocks needed in controllers
 *   - Consistent error response format (CustomErrorResponse JSON) across the API
 *   - Easy to add new exception types in one place
 *
 * Extends ResponseEntityExceptionHandler to inherit handling for standard
 * Spring MVC exceptions (e.g. MethodArgumentNotValidException, HttpMessageNotReadableException).
 */
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * handleProductNotFoundException — handles ProductNotFoundException → 404 Not Found.
     *
     * Called automatically by Spring when any controller method throws ProductNotFoundException.
     * Returns a structured CustomErrorResponse JSON body instead of Spring's default
     * error page/HTML.
     *
     * Example response:
     * {
     *   "status": "NOT_FOUND",
     *   "error": "Product not found",
     *   "message": "Product with given id doesn't exist"
     * }
     *
     * @param ex      the thrown ProductNotFoundException (contains the error message)
     * @param request the web request that triggered the exception (for context/logging)
     * @return 404 Not Found with a CustomErrorResponse body
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleProductNotFoundException(ProductNotFoundException ex, WebRequest request) {
        // Build a structured error response with status, a user-friendly message, and the exception detail
        CustomErrorResponse customErrorResponse = new CustomErrorResponse(
                HttpStatus.NOT_FOUND,       // HTTP status code (404)
                "Product not found",        // High-level error description
                ex.getMessage()             // Detailed message from the exception (e.g. "Product with given id doesn't exist")
        );
        return new ResponseEntity<>(customErrorResponse, HttpStatus.NOT_FOUND);
    }
}
