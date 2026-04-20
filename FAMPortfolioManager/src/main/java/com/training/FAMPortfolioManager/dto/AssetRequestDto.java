package com.training.FAMPortfolioManager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Incoming request body for creating or updating an asset.
// Validation annotations ensure bad data is rejected before reaching the service layer.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetRequestDto {

    @NotBlank(message = "Ticker required")
    private String ticker;

    @NotBlank(message = "Company name required")
    private String companyName;

    @Positive(message = "Quantity must be positive")
    private double quantity;

    @Positive(message = "Price must be positive")
    private double purchasePrice;

    @NotNull(message = "Purchase date required")
    private LocalDateTime purchaseDate;

    // The portfolio this asset should belong to
    @NotNull(message = "Portfolio ID required")
    private Long portfolioId;
}