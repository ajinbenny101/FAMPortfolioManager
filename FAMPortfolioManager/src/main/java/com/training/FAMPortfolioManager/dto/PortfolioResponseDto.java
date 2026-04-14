package com.training.FAMPortfolioManager.dto;

// PortfolioResponseDto - full portfolio summary
// CLASS ANNOTATIONS: (none, this is a plain POJO)
// FIELD ANNOTATIONS:
//   @Getter, @Setter - Lombok annotations for automatic getters/setters
//   @NoArgsConstructor - Lombok creates no-arg constructor
//   @AllArgsConstructor - Lombok creates constructor with all fields
// Fields:
//   totalValue (double) - sum of all asset market values
//   totalReturn (double) - total profit/loss across all assets
//   totalReturnPercent (double) - return as percentage
//   assetTypeBreakdown (Map<AssetType, Double>) - sum of market value by type for donut chart
// Returned by GET /api/portfolio/summary endpoint
//
// FIELD LAYOUT:
// private double totalValue;                          // Sum of all (quantity * currentPrice)
// private double totalReturn;                         // Sum of all profitLoss values
// private double totalReturnPercent;                  // (totalReturn / totalCost) * 100
// private Map<AssetType, Double> assetTypeBreakdown; // Market value grouped by STOCK, BOND, CRYPTO, CASH
//
// IMPORTS NEEDED:
// import com.training.FAMPortfolioManager.model.AssetType;
// import java.util.Map;
// import lombok.Getter;
// import lombok.Setter;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDto {

    private List<AssetResponseDto> assets;
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private double totalValue;
    private double totalReturn;
    private double totalReturnPercent;
    private int totalAssets;
}