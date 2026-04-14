package com.training.FAMPortfolioManager.dto;

// AssetRequestDto - DTO for asset creation/update requests from frontend
// CLASS ANNOTATIONS: (none, this is a plain POJO)
// FIELD ANNOTATIONS:
//   @NotBlank(message = "...") - String field must not be null or empty
//   @Positive(message = "...") - numeric field must be > 0
//   @NotNull(message = "...") - field must not be null
//   @Getter, @Setter - Lombok annotations for automatic getters/setters
//   @NoArgsConstructor - Lombok creates no-arg constructor
//   @AllArgsConstructor - Lombok creates constructor with all fields
// Fields: ticker, companyName, quantity, purchasePrice, purchaseDate, assetType
// Add validation annotations: @NotNull, @Positive, @NotBlank as appropriate
// Used when frontend sends data to POST /api/assets endpoint
//
// FIELD LAYOUT:
// @NotBlank(message = "Ticker required")
// private String ticker;
// @NotBlank(message = "Company name required")
// private String companyName;
// @Positive(message = "Quantity must be positive")
// private double quantity;
// @Positive(message = "Price must be positive")
// private double purchasePrice;
// @NotNull(message = "Purchase date required")
// private LocalDateTime purchaseDate;
// @NotNull(message = "Asset type required")
// private AssetType assetType;
//
// IMPORTS NEEDED:
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @NotNull(message = "Asset type required")
    private Long portfolioId;
}