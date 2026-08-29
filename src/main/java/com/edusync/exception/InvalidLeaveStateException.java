package com.edusync.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid transition in the leave approval workflow is attempted.
 * Maps to HTTP 409 CONFLICT or 422 UNPROCESSABLE_ENTITY.
 */
public class InvalidLeaveStateException extends BaseException {

    public InvalidLeaveStateException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
