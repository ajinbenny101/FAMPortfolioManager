package com.training.FAMPortfolioManager.service;

import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;
import com.training.FAMPortfolioManager.dto.PortfolioRequestDTO;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Business logic for portfolio CRUD and portfolio-level performance calculations.
// Coordinates between PortfolioRepository, AssetRepository, and PriceService.
@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final PriceService priceService;

    public PortfolioService(PortfolioRepository portfolioRepository,
            AssetRepository assetRepository,
            PriceService priceService) {
        this.portfolioRepository = portfolioRepository;
        this.assetRepository = assetRepository;
        this.priceService = priceService;
    }

    // CREATE - saves a new portfolio with the current timestamp
    public PortfolioResponseDto addPortfolio(PortfolioRequestDTO request) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setCreatedDate(java.time.LocalDateTime.now());
        Portfolio saved = portfolioRepository.save(portfolio);
        return mapToResponse(saved);
    }

    // READ ALL - returns all portfolios with their assets
    public List<PortfolioResponseDto> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // READ ONE - returns a single portfolio or throws if not found
    public PortfolioResponseDto getPortfolioById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        return mapToResponse(portfolio);
    }

    // UPDATE - changes the name and description of an existing portfolio
    public PortfolioResponseDto updatePortfolio(Long id, PortfolioRequestDTO request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        Portfolio updated = portfolioRepository.save(portfolio);
        return mapToResponse(updated);
    }

    // DELETE - removes the portfolio (cascades to its assets via CascadeType.ALL)
    public void deletePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        portfolioRepository.delete(portfolio);
    }

    // PERFORMANCE (single portfolio) - monthly value series for assets in one portfolio
    public List<PerformanceDataPointDto> getPortfolioPerformance(Long portfolioId) {
        List<Asset> assets = assetRepository.findByPortfolioId(portfolioId);
        return buildPerformanceSeries(assets);
    }

    // PERFORMANCE (overall) - monthly value series combining all assets across all portfolios
    public List<PerformanceDataPointDto> getOverallPerformance() {
        List<Asset> assets = assetRepository.findAll();
        return buildPerformanceSeries(assets);
    }

    // Builds a month-by-month value series from the earliest purchase month to now.
    // Only assets that have been purchased by a given month are included in that month's total.
    private List<PerformanceDataPointDto> buildPerformanceSeries(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }

        // Filter out assets that have no purchase date
        List<Asset> datedAssets = assets.stream()
                .filter(a -> a.getDatePurchased() != null)
                .collect(Collectors.toList());

        if (datedAssets.isEmpty()) {
            return List.of();
        }

        // Determine the range: earliest purchase month → current month
        YearMonth startMonth = datedAssets.stream()
                .map(a -> YearMonth.from(a.getDatePurchased().toLocalDate()))
                .min(YearMonth::compareTo)
                .orElse(YearMonth.now());

        YearMonth endMonth = YearMonth.now();

        // Build a map of ticker → earliest purchase date so we fetch history once per ticker
        Map<String, LocalDate> earliestByTicker = new HashMap<>();
        for (Asset asset : datedAssets) {
            String ticker = asset.getTicker() == null ? null : asset.getTicker().trim().toUpperCase();
            if (ticker == null || ticker.isBlank()) {
                continue;
            }
            LocalDate purchaseDate = asset.getDatePurchased().toLocalDate();
            LocalDate existing = earliestByTicker.get(ticker);
            if (existing == null || purchaseDate.isBefore(existing)) {
                earliestByTicker.put(ticker, purchaseDate);
            }
        }

        // Ensure monthly price history is stored in the DB for each unique ticker
        for (Map.Entry<String, LocalDate> entry : earliestByTicker.entrySet()) {
            priceService.ensureMonthlySeriesStored(entry.getKey(), entry.getValue(), LocalDate.now());
        }

        // Walk month by month and sum up the value of all assets held that month
        List<PerformanceDataPointDto> series = new ArrayList<>();
        YearMonth cursor = startMonth;

        while (!cursor.isAfter(endMonth)) {
            LocalDate monthDate = cursor.atDay(1);
            double totalValue = 0.0;

            for (Asset asset : datedAssets) {
                // Skip assets not yet purchased in this month
                YearMonth purchaseMonth = YearMonth.from(asset.getDatePurchased().toLocalDate());
                if (cursor.isBefore(purchaseMonth)) {
                    continue;
                }

                double unitPrice = resolveMonthlyPrice(asset, monthDate, cursor.equals(endMonth));
                totalValue += unitPrice * asset.getQuantity();
            }

            series.add(new PerformanceDataPointDto(monthDate, round(totalValue)));
            cursor = cursor.plusMonths(1);
        }

        return series;
    }

    // Returns the best available price for a given month:
    //   1. Stored monthly close price from the DB
    //   2. Live price (current month only)
    //   3. Purchase price as last resort
    private double resolveMonthlyPrice(Asset asset, LocalDate monthDate, boolean isCurrentMonth) {
        try {
            Double stored = priceService.getMonthlyClosePrice(asset.getTicker(), monthDate);
            if (stored != null && stored > 0) {
                return stored;
            }
        } catch (RuntimeException ignored) {
            // Falls through to next fallback.
        }

        if (isCurrentMonth) {
            try {
                return priceService.getCurrentPrice(asset.getTicker());
            } catch (RuntimeException ignored) {
                // Falls through to purchase price.
            }
        }

        return asset.getPurchasePrice();
    }

    // Rounds a value to 2 decimal places
    private double round(double value) {
        return new java.math.BigDecimal(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    // Maps a Portfolio entity to the response DTO, including a list of its assets
    private PortfolioResponseDto mapToResponse(Portfolio portfolio) {
        PortfolioResponseDto dto = new PortfolioResponseDto();
        dto.setId(portfolio.getId());
        dto.setName(portfolio.getName());
        dto.setDescription(portfolio.getDescription());
        dto.setCreatedDate(portfolio.getCreatedDate());

        List<AssetResponseDto> assets = portfolio.getAssets()
                .stream()
                .map(asset -> {
                    AssetResponseDto a = new AssetResponseDto();
                    a.setId(asset.getId());
                    a.setCompanyName(asset.getCompanyName());
                    a.setTicker(asset.getTicker());
                    a.setQuantity(asset.getQuantity());
                    a.setPurchasePrice(asset.getPurchasePrice());
                    a.setPurchaseDate(asset.getDatePurchased());
                    return a;
                })
                .collect(Collectors.toList());

        dto.setAssets(assets);
        return dto;
    }
}