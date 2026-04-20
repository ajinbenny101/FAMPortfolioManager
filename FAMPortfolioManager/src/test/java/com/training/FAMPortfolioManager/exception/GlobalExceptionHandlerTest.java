package com.training.FAMPortfolioManager.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    

    @Test
    void handleAssetNotFound_returns404Payload() {
        AssetNotFoundException ex = new AssetNotFoundException(42L);

        ResponseEntity<Map<String, Object>> response = handler.handleAssetNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Asset not found with id: 42", response.getBody().get("message"));
        assertEquals(404, response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleGlobalException_returns500Payload() {
        Exception ex = new Exception("boom");

        ResponseEntity<Map<String, Object>> response = handler.handleGlobalException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().get("message"));
        assertEquals("boom", response.getBody().get("details"));
        assertEquals(500, response.getBody().get("status"));
    }
}
