package com.training.FAMPortfolioManager.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Response body returned for any asset-related GET endpoint.
// Includes the live price and calculated profit/loss fields computed by AssetService.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponseDto {
    private Long id;
    private String ticker;
    private String companyName;
    private double quantity;
    private double purchasePrice;
    private LocalDateTime purchaseDate;
    private double currentPrice;       // Live market price fetched from PriceService
    private double marketValue;        // quantity * currentPrice
    private double profitLoss;         // (currentPrice - purchasePrice) * quantity
    private double profitLossPercent;  // profitLoss / (purchasePrice * quantity) * 100
}
