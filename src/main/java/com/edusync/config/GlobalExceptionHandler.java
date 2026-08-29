package com.edusync.config;

import com.edusync.dto.AppDto;
import com.edusync.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler.
 * Returns consistent JSON error responses with accurate HTTP status codes for all API errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Domain-specific business exceptions (404, 409, 403, 400, 422) ────

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleBaseException(BaseException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(AppDto.ApiResponse.error(ex.getMessage()));
    }

    // ─── Validation errors (e.g. @NotBlank, @Email) ───────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(AppDto.ApiResponse.error("Validation failed: " + errors));
    }

    // ─── Wrong email/password ─────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(AppDto.ApiResponse.error("Invalid email or password."));
    }

    // ─── Access denied (wrong role) ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(AppDto.ApiResponse.error("Access denied: insufficient permissions."));
    }

    // ─── Fallback runtime exceptions ──────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(AppDto.ApiResponse.error(ex.getMessage()));
    }

    // ─── Catch-all ────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AppDto.ApiResponse.error("Internal server error: " + ex.getMessage()));
    }
}
