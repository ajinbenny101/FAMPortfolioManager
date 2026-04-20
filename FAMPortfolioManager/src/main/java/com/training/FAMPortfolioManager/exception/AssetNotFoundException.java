package com.training.FAMPortfolioManager.exception;

// Thrown by AssetService when a requested asset ID does not exist in the database.
// Extending RuntimeException means callers don't need to declare it in a throws clause.
public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(Long id) {
        super("Asset not found with id: " + id);
    }
}