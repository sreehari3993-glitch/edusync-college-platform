package com.edusync.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when business validation fails (e.g. incorrect current password, negative marks, missing required parameters).
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
