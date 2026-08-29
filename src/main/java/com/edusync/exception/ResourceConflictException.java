package com.edusync.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a resource conflict occurs (e.g. duplicate email, duplicate drive application).
 * Maps to HTTP 409 CONFLICT.
 */
public class ResourceConflictException extends BaseException {

    public ResourceConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
