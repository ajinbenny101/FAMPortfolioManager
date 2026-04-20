package com.training.FAMPortfolioManager.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.training.FAMPortfolioManager.model.AssetMonthlyPrice;

// Data access for AssetMonthlyPrice entities.
// Used by PriceService to check coverage and look up stored monthly close prices.
public interface AssetMonthlyPriceRepository extends JpaRepository<AssetMonthlyPrice, Long> {

    // Find an exact record for a ticker on a given month start date
    Optional<AssetMonthlyPrice> findByTickerAndPriceDate(String ticker, LocalDate priceDate);

    // Find the oldest stored price for a ticker (used to check coverage start)
    AssetMonthlyPrice findTopByTickerOrderByPriceDateAsc(String ticker);

    // Find the newest stored price for a ticker (used to check coverage end)
    AssetMonthlyPrice findTopByTickerOrderByPriceDateDesc(String ticker);

    // Find the most recent stored price on or before a given month date (for performance lookups)
    AssetMonthlyPrice findTopByTickerAndPriceDateLessThanEqualOrderByPriceDateDesc(String ticker, LocalDate priceDate);
}
