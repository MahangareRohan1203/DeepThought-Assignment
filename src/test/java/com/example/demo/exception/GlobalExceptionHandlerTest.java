package com.example.demo.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCustomException_ReturnsCorrectStatus() {
        CustomException ex = new CustomException("ERR_CODE", "Some Message", HttpStatus.BAD_GATEWAY);
        ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("ERR_CODE", response.getBody().getError());
        assertEquals("Some Message", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_DuplicateKey_ReturnsConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key violates unique constraint");
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DUPLICATE_RESOURCE", response.getBody().getError());
    }

    @Test
    void handleNotFoundException_Returns404() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/test", null);
        ResponseEntity<ErrorResponse> response = handler.handleNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ENDPOINT_NOT_FOUND", response.getBody().getError());
    }

    @Test
    void handleGenericException_MasksMessage() {
        Exception ex = new RuntimeException("Secret SQL details");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
        assertNotEquals("Secret SQL details", response.getBody().getMessage());
        assertEquals("An unexpected error occurred. Please contact support.", response.getBody().getMessage());
    }
}
