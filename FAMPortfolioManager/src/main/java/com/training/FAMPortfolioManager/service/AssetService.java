package com.training.FAMPortfolioManager.service;

import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;
import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.stream.Collectors;

// Business logic for asset CRUD and per-asset performance calculations.
// Coordinates between AssetRepository, PortfolioRepository, and PriceService.
@Service
public class AssetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    private final PriceService priceService;

    public AssetService(AssetRepository assetRepository, PortfolioRepository portfolioRepository, PriceService priceService) {
        this.assetRepository = assetRepository;
        this.portfolioRepository = portfolioRepository;
        this.priceService = priceService;
    }

    // CREATE - saves a new asset linked to the given portfolio
    public AssetResponseDto addAsset(AssetRequestDto request) {

        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        Asset asset = new Asset();
        asset.setCompanyName(request.getCompanyName());
        asset.setTicker(request.getTicker());
        asset.setQuantity(request.getQuantity());
        asset.setPurchasePrice(request.getPurchasePrice());
        asset.setDatePurchased(request.getPurchaseDate());
        asset.setPortfolio(portfolio);

        Asset saved = assetRepository.save(asset);
        return mapToResponse(saved);
    }

    // READ ALL - returns all assets belonging to a specific portfolio
    public List<AssetResponseDto> getAssetsByPortfolio(Long portfolioId) {
        return assetRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // READ ONE - returns a single asset or throws if not found
    public AssetResponseDto getAssetById(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        return mapToResponse(asset);
    }

    // PERFORMANCE - builds a month-by-month value series from the asset's purchase date to now.
    // Ensures monthly price history is stored in the DB before building the series.
    public List<PerformanceDataPointDto> getAssetPerformance(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        if (asset.getDatePurchased() == null) {
            return List.of();
        }

        LocalDate purchaseDate = asset.getDatePurchased().toLocalDate();
        YearMonth startMonth = YearMonth.from(purchaseDate);
        YearMonth endMonth = YearMonth.now();

        // Fetch and store historical monthly prices if not already cached in the DB
        priceService.ensureMonthlySeriesStored(asset.getTicker(), purchaseDate, LocalDate.now());

        List<PerformanceDataPointDto> series = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            LocalDate monthDate = cursor.atDay(1);
            double unitPrice = resolveMonthlyPrice(asset, monthDate, cursor.equals(endMonth));
            double totalValue = round(unitPrice * asset.getQuantity());
            series.add(new PerformanceDataPointDto(monthDate, totalValue));
            cursor = cursor.plusMonths(1);
        }

        return series;
    }

    // UPDATE - updates all fields; moves the asset to a different portfolio if the ID changed
    public AssetResponseDto updateAsset(Long assetId, AssetRequestDto request) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setCompanyName(request.getCompanyName());
        asset.setTicker(request.getTicker());
        asset.setQuantity(request.getQuantity());
        asset.setPurchasePrice(request.getPurchasePrice());
        asset.setDatePurchased(request.getPurchaseDate());

        // Reassign portfolio only if it has actually changed
        if (!asset.getPortfolio().getId().equals(request.getPortfolioId())) {
            Portfolio newPortfolio = portfolioRepository.findById(request.getPortfolioId())
                    .orElseThrow(() -> new RuntimeException("Portfolio not found"));
            asset.setPortfolio(newPortfolio);
        }

        Asset updated = assetRepository.save(asset);
        return mapToResponse(updated);
    }

    // DELETE - removes the asset from the database
    public void deleteAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        assetRepository.delete(asset);
    }

    // Maps an Asset entity to the response DTO, including live price and P/L calculations
    private AssetResponseDto mapToResponse(Asset asset) {

        double currentPrice = resolveCurrentPrice(asset);
        double marketValue = round(currentPrice * asset.getQuantity());
        double profitLoss = round((currentPrice - asset.getPurchasePrice()) * asset.getQuantity());
        double profitLossPercent = 0.0;

        if (asset.getPurchasePrice() > 0 && asset.getQuantity() > 0) {
            profitLossPercent = round((profitLoss / (asset.getPurchasePrice() * asset.getQuantity())) * 100.0);
        }

        AssetResponseDto dto = new AssetResponseDto();
        dto.setId(asset.getId());
        dto.setCompanyName(asset.getCompanyName());
        dto.setTicker(asset.getTicker());
        dto.setQuantity(asset.getQuantity());
        dto.setPurchasePrice(asset.getPurchasePrice());
        dto.setPurchaseDate(asset.getDatePurchased());
        dto.setCurrentPrice(currentPrice);
        dto.setMarketValue(marketValue);
        dto.setProfitLoss(profitLoss);
        dto.setProfitLossPercent(profitLossPercent);

        return dto;
    }

    // Fetches the current live price; falls back to purchase price if the provider fails
    private double resolveCurrentPrice(Asset asset) {
        try {
            return round(priceService.getCurrentPrice(asset.getTicker()));
        } catch (RuntimeException ex) {
            LOGGER.warn("Falling back to purchase price for ticker {}: {}", asset.getTicker(), ex.getMessage());
            return round(asset.getPurchasePrice());
        }
    }

    // Returns the best available price for a given month:
    //   1. Stored monthly close price from the DB
    //   2. Live price (current month only)
    //   3. Purchase price as last resort
    private double resolveMonthlyPrice(Asset asset, LocalDate monthDate, boolean isCurrentMonth) {
        try {
            Double stored = priceService.getMonthlyClosePrice(asset.getTicker(), monthDate);
            if (stored != null && stored > 0) {
                return round(stored);
            }
        } catch (RuntimeException ignored) {
            // Continue to fallback.
        }

        if (isCurrentMonth) {
            return resolveCurrentPrice(asset);
        }

        return round(asset.getPurchasePrice());
    }

    // Rounds a value to 2 decimal places
    private double round(double value) {
        return new java.math.BigDecimal(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}