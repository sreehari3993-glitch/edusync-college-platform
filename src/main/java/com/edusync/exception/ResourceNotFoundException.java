package com.edusync.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource (User, LeaveRequest, PlacementDrive, Assignment, etc.) cannot be found.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s not found: %s", resourceName, id), HttpStatus.NOT_FOUND);
    }
}
