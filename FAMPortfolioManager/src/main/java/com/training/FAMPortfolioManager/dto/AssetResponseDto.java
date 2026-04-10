package com.training.FAMPortfolioManager.dto;

// AssetResponseDto - DTO for asset response from API
// CLASS ANNOTATIONS: (none, this is a plain POJO)
// FIELD ANNOTATIONS:
//   @Getter, @Setter - Lombok annotations for automatic getters/setters
//   @NoArgsConstructor - Lombok creates no-arg constructor
//   @AllArgsConstructor - Lombok creates constructor with all fields
// Contains all AssetRequestDto fields plus:
//   id (Long) - asset identifier
//   currentPrice (double) - fetched from PriceService
//   marketValue (double) - quantity * currentPrice
//   profitLoss (double) - calculated by service layer
// Used for GET endpoints to return complete asset information
//
// FIELD LAYOUT:
// private Long id;
// private String ticker;
// private String companyName;
// private double quantity;
// private double purchasePrice;
// private LocalDateTime purchaseDate;
// private AssetType assetType;
// private double currentPrice;        // Added: live market price
// private double marketValue;         // Added: quantity * currentPrice
// private double profitLoss;          // Added: (currentPrice - purchasePrice) * quantity
// private double profitLossPercent;   // Added: (profitLoss / totalCost) * 100
//
// IMPORTS NEEDED:
// import com.training.FAMPortfolioManager.model.AssetType;
// import java.time.LocalDateTime;
// import lombok.Getter;
// import lombok.Setter;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
public class AssetResponseDto {

}