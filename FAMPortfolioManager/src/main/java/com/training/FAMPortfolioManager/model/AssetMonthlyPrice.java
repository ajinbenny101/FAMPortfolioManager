package com.training.FAMPortfolioManager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Stores one closing price per ticker per calendar month, fetched from Twelve Data.
// The unique constraint on (ticker, price_date) prevents duplicate entries.
// Used by PriceService to build the historical performance charts without re-fetching from the API.
@Entity
@Table(
        name = "asset_monthly_prices",
        uniqueConstraints = @UniqueConstraint(name = "uk_asset_monthly_price_ticker_date", columnNames = { "ticker", "price_date" }),
        indexes = {
                @Index(name = "idx_asset_monthly_price_ticker_date", columnList = "ticker,price_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetMonthlyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String ticker;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "close_price", nullable = false)
    private double closePrice;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
