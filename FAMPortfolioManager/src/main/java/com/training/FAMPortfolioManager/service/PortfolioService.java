package com.training.FAMPortfolioManager.service;

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
// IMPORTS NEEDED:
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.AssetType;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PriceService priceService;

    public PortfolioResponseDto getSummary() {
        Portfolio portfolio = portfolioRepository.findFirstPortfolio()
                .orElseThrow(() -> new RuntimeException("No portfolio found"));

        List<Asset> assets = portfolio.getAssets();
        double totalValue = 0.0;
        double totalReturn = 0.0;
        double totalCost = 0.0;
        Map<AssetType, Double> assetTypeBreakdown = new HashMap<>();

        for (Asset asset : assets) {
            double currentPrice = priceService.getCurrentPrice(asset.getTicker());
            double marketValue = asset.getQuantity() * currentPrice;
            double costBasis = asset.getQuantity() * asset.getPurchasePrice();
            double profitLoss = marketValue - costBasis;

            totalValue += marketValue;
            totalReturn += profitLoss;
            totalCost += costBasis;

            // Add to asset type breakdown
            assetTypeBreakdown.merge(asset.getAssetType(), marketValue, Double::sum);
        }

        double totalReturnPercent = totalCost > 0 ? (totalReturn / totalCost) * 100 : 0.0;

        return new PortfolioResponseDto(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getDescription(),
                portfolio.getCreatedDate(),
                totalValue,
                totalReturn,
                totalReturnPercent,
                assetTypeBreakdown,
                assets.size()
        );
    }

    public List<PerformanceDataPointDto> getPerformance() {
        List<Asset> assets = assetRepository.findAll();

        // Group assets by purchase date and calculate performance
        Map<LocalDate, List<Asset>> assetsByDate = assets.stream()
                .collect(Collectors.groupingBy(asset -> asset.getPurchaseDate().toLocalDate()));

        return assetsByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Asset> dateAssets = entry.getValue();

                    double totalValue = 0.0;
                    double totalCost = 0.0;

                    for (Asset asset : dateAssets) {
                        double currentPrice = priceService.getCurrentPrice(asset.getTicker());
                        totalValue += asset.getQuantity() * currentPrice;
                        totalCost += asset.getQuantity() * asset.getPurchasePrice();
                    }

                    double profitLoss = totalValue - totalCost;
                    double profitLossPercent = totalCost > 0 ? (profitLoss / totalCost) * 100 : 0.0;

                    return new PerformanceDataPointDto(date, totalValue, profitLoss, profitLossPercent);
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    public Portfolio createPortfolio(String name, String description) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setDescription(description);
        portfolio.setCreatedDate(LocalDateTime.now());

        return portfolioRepository.save(portfolio);
    }

    public Portfolio getPortfolio() {
        return portfolioRepository.findFirstPortfolio()
                .orElseThrow(() -> new RuntimeException("No portfolio found"));
    }
}