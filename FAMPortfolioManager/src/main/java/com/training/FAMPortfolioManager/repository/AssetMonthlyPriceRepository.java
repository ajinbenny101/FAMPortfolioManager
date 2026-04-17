package com.training.FAMPortfolioManager.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.training.FAMPortfolioManager.model.AssetMonthlyPrice;

public interface AssetMonthlyPriceRepository extends JpaRepository<AssetMonthlyPrice, Long> {

    Optional<AssetMonthlyPrice> findByTickerAndPriceDate(String ticker, LocalDate priceDate);

    AssetMonthlyPrice findTopByTickerOrderByPriceDateAsc(String ticker);

    AssetMonthlyPrice findTopByTickerOrderByPriceDateDesc(String ticker);

    AssetMonthlyPrice findTopByTickerAndPriceDateLessThanEqualOrderByPriceDateDesc(String ticker, LocalDate priceDate);

    List<AssetMonthlyPrice> findByTickerAndPriceDateBetweenOrderByPriceDateAsc(String ticker, LocalDate startDate, LocalDate endDate);
}
