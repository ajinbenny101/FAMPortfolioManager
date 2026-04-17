package com.training.FAMPortfolioManager.exception;

// GlobalExceptionHandler - centralized exception handling
// Annotate with @ControllerAdvice
// @ExceptionHandler method for AssetNotFoundException:
//   - Method name: handleAssetNotFound(AssetNotFoundException ex)
//   - Returns ResponseEntity<?> with status 404 and JSON error message
//   - Create error response: { "message": ex.getMessage(), "timestamp": LocalDateTime.now() }
// @ExceptionHandler method for general Exception:
//   - Method name: handleGlobalException(Exception ex)
//   - Returns ResponseEntity<?> with status 500 and generic error message
//   - Create error response: { "message": "Internal server error", "details": ex.getMessage() }

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


// This class provides centralized exception handling for the application. 
// It catches specific exceptions like AssetNotFoundException and general exceptions, 
// returning appropriate HTTP status codes and error messages in JSON format to the client.
@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle AssetNotFoundException and return 404 status with error message
    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssetNotFound(AssetNotFoundException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Handle any other exceptions and return 500 status with generic error message
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "Internal server error");
        errorResponse.put("details", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}