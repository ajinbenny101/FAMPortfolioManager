package com.training.FAMPortfolioManager.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Response body returned for portfolio endpoints.
// Includes the portfolio's metadata and the full list of its assets.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDto {

    private List<AssetResponseDto> assets;
    private Long Id;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private double totalValue;
    private double totalReturn;
    private double totalReturnPercent;
    private int totalAssets;
}