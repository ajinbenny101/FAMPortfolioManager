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
//
// IMPORTS NEEDED:
// import org.springframework.web.bind.annotation.ControllerAdvice;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.http.ResponseEntity;
// import org.springframework.http.HttpStatus;
// import java.time.LocalDateTime;
// import java.util.HashMap;
// import java.util.Map;
// IMPORTS NEEDED:
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssetNotFound(AssetNotFoundException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

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