package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global error handler that converts exceptions into a consistent JSON shape.
 *
 * Why this exists:
 * - Without a central handler, Spring's default error responses expose a stack trace and a
 *   verbose "timestamp / path / message" object that is confusing for API consumers.
 * - This class ensures every error response looks the same: {"error": "..."}.
 * - Validation failures produce a list of human-readable field messages; all other unexpected
 *   errors produce a single polite message so internal details are never leaked.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid failures on @RequestBody DTOs.
     *
     * Returns 400 Bad Request with the list of field-level messages so the client
     * knows exactly what to fix (e.g., "name: Celebrity name must not be blank").
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> messages = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", messages.size() == 1 ? messages.get(0) : messages);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles missing required @RequestParam values.
     *
     * Returns 400 Bad Request so clients know which parameter was omitted,
     * instead of falling through to the generic 500 catch-all.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Missing required parameter: " + ex.getParameterName());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Catch-all for any unhandled runtime exception.
     *
     * Returns 500 Internal Server Error with a generic message.
     * The actual exception is intentionally not included in the response to avoid
     * leaking stack traces or internal implementation details to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "An unexpected error occurred. Please try again.");
        return ResponseEntity.internalServerError().body(body);
    }
}
