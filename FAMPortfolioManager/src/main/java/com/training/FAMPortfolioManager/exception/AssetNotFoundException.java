package com.training.FAMPortfolioManager.exception;

// AssetNotFoundException - custom exception for missing assets

// CLASS ANNOTATIONS: (none - exceptions don't need Spring annotations)
// Extend RuntimeException (unchecked exception - no need to declare throws)
// Constructor takes an asset id and passes message like "Asset not found with id: {id}" to super
// Thrown when AssetRepository.findById() returns empty or asset not found
//
// CONSTRUCTOR:
// public AssetNotFoundException(Long id) {
//     super("Asset not found with id: " + id);
// }
//
// IMPORTS NEEDED:
// (No additional imports needed - RuntimeException is built-in)
public class AssetNotFoundException extends RuntimeException {
    // Constructor: public AssetNotFoundException(Long id)
    public AssetNotFoundException(Long id) {
        super("Asset not found with id: " + id);
    }
}