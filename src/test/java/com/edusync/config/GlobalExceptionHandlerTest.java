package com.edusync.config;

import com.edusync.dto.AppDto;
import com.edusync.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 42L);
        ResponseEntity<AppDto.ApiResponse<Void>> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not found: 42", response.getBody().getMessage());
    }

    @Test
    void testHandleResourceConflictException() {
        ResourceConflictException ex = new ResourceConflictException("Email already registered: student@tkmit.ac.in");
        ResponseEntity<AppDto.ApiResponse<Void>> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Email already registered: student@tkmit.ac.in", response.getBody().getMessage());
    }

    @Test
    void testHandleInvalidLeaveStateException() {
        InvalidLeaveStateException ex = new InvalidLeaveStateException("Leave request #5 is already processed");
        ResponseEntity<AppDto.ApiResponse<Void>> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Leave request #5 is already processed", response.getBody().getMessage());
    }

    @Test
    void testHandleForbiddenOperationException() {
        ForbiddenOperationException ex = new ForbiddenOperationException("Account is deactivated. Contact admin.");
        ResponseEntity<AppDto.ApiResponse<Void>> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Account is deactivated. Contact admin.", response.getBody().getMessage());
    }

    @Test
    void testHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Current password does not match.");
        ResponseEntity<AppDto.ApiResponse<Void>> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Current password does not match.", response.getBody().getMessage());
    }

    @Test
    void testHandleFileProcessingException() {
        FileProcessingException ex = new FileProcessingException("Failed to generate Leave Certificate PDF: Out of memory");
        ResponseEntity<AppDto.ApiResponse<Void>> response = exceptionHandler.handleBaseException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to generate Leave Certificate PDF: Out of memory", response.getBody().getMessage());
    }
}
