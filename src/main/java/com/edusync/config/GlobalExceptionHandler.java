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

    // ─── Deactivated account ───────────────────────────────────────────────
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleDisabledException(
            org.springframework.security.authentication.DisabledException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(AppDto.ApiResponse.error("Your account has been deactivated by administrator. Please contact admin@tkmit.ac.in."));
    }

    // ─── Access denied (wrong role) ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(AppDto.ApiResponse.error("Access denied: insufficient permissions."));
    }

    // ─── Data integrity / Unique constraint violations (e.g. duplicate email) ───

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<AppDto.ApiResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex
    ) {
        String msg = "A user or record with this unique identifier (email or roll number) already exists.";
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("email")) {
            msg = "An account with this email address is already registered in EduSync.";
        }
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(AppDto.ApiResponse.error(msg));
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
