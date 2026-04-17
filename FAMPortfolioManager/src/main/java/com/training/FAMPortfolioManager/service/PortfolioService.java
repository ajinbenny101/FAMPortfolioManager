package com.training.FAMPortfolioManager.service;

// import com.training.FAMPortfolioManager.model.Asset;
// PortfolioService - business logic for portfolio-wide operations
// Annotate with @Service
// DEPENDENCY INJECTION:
//   @Autowired or constructor injection of:
//     - PortfolioRepository portfolioRepository
//     - AssetRepository assetRepository
//     - PriceService priceService
//
// Methods:
//   PortfolioResponseDto getSummary() - loop all assets, fetch live prices, calculate totals and P/L
//     Return: PortfolioResponseDto with:
//       - totalValue (sum of all market values)
//       - totalReturn (sum of all profitLoss)
//       - totalReturnPercent (percentage return)
//       - assetTypeBreakdown (Map<AssetType, Double>)
//   List<PerformanceDataPointDto> getPerformance() - build list grouped by purchase date
//     Return: List<PerformanceDataPointDto> sorted by date for line chart
// Call PriceService.getCurrentPrice() for each asset
//
// IMPORTS NEEDED:
// import org.springframework.stereotype.Service;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.transaction.annotation.Transactional;
// import com.training.FAMPortfolioManager.repository.PortfolioRepository;
// import com.training.FAMPortfolioManager.repository.AssetRepository;
// import com.training.FAMPortfolioManager.model.Asset;
// import com.training.FAMPortfolioManager.model.AssetType;
// import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
// import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
// import java.util.List;
// import java.util.Map;
// import java.util.HashMap;
// import java.util.stream.Collectors;
// import java.util.stream.Collectors.groupingBy;
// import java.time.LocalDate;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.model.Asset;
// import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;

// import jakarta.transaction.Transactional;

import com.training.FAMPortfolioManager.dto.PortfolioRequestDTO;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
// import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;

import org.springframework.stereotype.Service;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.HashMap;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    //WHEN BUISNESS LOGIC METHODS ARE ADDED, UNCOMMENT BELOW CONSTRUCTOR AND FIELDS
    // public PortfolioService(PortfolioRepository portfolioRepository, AssetRepository assetRepository, PriceService priceService) {
    //     this.portfolioRepository = portfolioRepository;
    //     this.assetRepository = assetRepository;
    //     this.priceService = priceService;
    // }

    // CREATE
    public PortfolioResponseDto addPortfolio(PortfolioRequestDTO request) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setCreatedDate(java.time.LocalDateTime.now());
        Portfolio saved = portfolioRepository.save(portfolio);
        return mapToResponse(saved);
    }

    // READ ALL
    public List<PortfolioResponseDto> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // READ ONE
    public PortfolioResponseDto getPortfolioById(Long id) {

        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        return mapToResponse(portfolio);
    }

    // UPDATE
    public PortfolioResponseDto updatePortfolio(Long id, PortfolioRequestDTO request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        Portfolio updated = portfolioRepository.save(portfolio);
        return mapToResponse(updated);
    }


    // DELETE
    public void deletePortfolio(Long id) {

        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        portfolioRepository.delete(portfolio);
    }

    public List<PerformanceDataPointDto> getPortfolioPerformance(Long portfolioId) {
        List<Asset> assets = assetRepository.findByPortfolioId(portfolioId);
        return buildPerformanceSeries(assets);
    }

    public List<PerformanceDataPointDto> getOverallPerformance() {
        List<Asset> assets = assetRepository.findAll();
        return buildPerformanceSeries(assets);
    }

    private List<PerformanceDataPointDto> buildPerformanceSeries(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }

        List<Asset> datedAssets = assets.stream()
                .filter(a -> a.getDatePurchased() != null)
                .collect(Collectors.toList());

        if (datedAssets.isEmpty()) {
            return List.of();
        }

        YearMonth startMonth = datedAssets.stream()
                .map(a -> YearMonth.from(a.getDatePurchased().toLocalDate()))
                .min(YearMonth::compareTo)
                .orElse(YearMonth.now());

        YearMonth endMonth = YearMonth.now();

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

        for (Map.Entry<String, LocalDate> entry : earliestByTicker.entrySet()) {
            priceService.ensureMonthlySeriesStored(entry.getKey(), entry.getValue(), LocalDate.now());
        }

        List<PerformanceDataPointDto> series = new ArrayList<>();
        YearMonth cursor = startMonth;

        while (!cursor.isAfter(endMonth)) {
            LocalDate monthDate = cursor.atDay(1);
            double totalValue = 0.0;

            for (Asset asset : datedAssets) {
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

    private double round(double value) {
        return new java.math.BigDecimal(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    //     // BUSINESS LOGIC: getSummary
    // @Transactional(readOnly = true)
    // public PortfolioResponseDto getSummary() {
    //     List<Asset> allAssets = assetRepository.findAll(); // Assuming findAll() exists
    //     double totalValue = 0.0;
    //     double totalReturn = 0.0;
    //     Map<AssetType, Double> assetTypeBreakdown = new HashMap<>();
        
    //     for (Asset asset : allAssets) {
    //         double currentPrice = priceService.getCurrentPrice(asset.getTicker());
    //         double marketValue = asset.getQuantity() * currentPrice;
    //         double profitLoss = marketValue - (asset.getQuantity() * asset.getPurchasePrice());
    //         totalValue += marketValue;
    //         totalReturn += profitLoss;
    //         assetTypeBreakdown.merge(asset.getAssetType(), marketValue, Double::sum);
    //     }
        
    //     double totalReturnPercent = totalValue > 0 ? (totalReturn / (totalValue - totalReturn)) * 100 : 0.0;
        
    //     PortfolioResponseDto response = new PortfolioResponseDto();
    //     response.setTotalValue(totalValue);
    //     response.setTotalReturn(totalReturn);
    //     response.setTotalReturnPercent(totalReturnPercent);
    //     response.setAssetTypeBreakdown(assetTypeBreakdown);
    //     return response;
    // }

    // // BUSINESS LOGIC: getPerformance
    // @Transactional(readOnly = true)
    // public List<PerformanceDataPointDto> getPerformance() {
    //     List<Asset> allAssets = assetRepository.findAll();
    //     return allAssets.stream()
    //             .collect(Collectors.groupingBy(Asset::getDatePurchased))
    //             .entrySet().stream()
    //             .map(entry -> {
    //                 LocalDate date = entry.getKey();
    //                 double totalValue = entry.getValue().stream()
    //                         .mapToDouble(asset -> asset.getQuantity() * priceService.getCurrentPrice(asset.getTicker()))
    //                         .sum();
    //                 PerformanceDataPointDto point = new PerformanceDataPointDto();
    //                 point.setDate(date);
    //                 point.setValue(totalValue);
    //                 return point;
    //             })
    //             .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
    //             .collect(Collectors.toList());
    // }
    // MAPPING
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