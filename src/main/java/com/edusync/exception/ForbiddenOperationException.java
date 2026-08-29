package com.edusync.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an operation outside their authorized departmental scope or on a deactivated account.
 * Maps to HTTP 403 FORBIDDEN.
 */
public class ForbiddenOperationException extends BaseException {

    public ForbiddenOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
