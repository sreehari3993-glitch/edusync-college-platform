package com.edusync.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an error occurs during PDF report generation, parsing, or file exports.
 * Maps to HTTP 422 UNPROCESSABLE_ENTITY.
 */
public class FileProcessingException extends BaseException {

    public FileProcessingException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
