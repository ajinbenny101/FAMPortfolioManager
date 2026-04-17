package com.training.FAMPortfolioManager.service;

import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;
import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;
// AssetService - business logic for asset management
// Annotate with @Service
// DEPENDENCY INJECTION:
//   @Autowired or constructor injection of:
//     - AssetRepository assetRepository
//     - PriceService priceService
//     - (Optional) PortfolioRepository portfolioRepository
//
// Methods:
//   List<AssetResponseDto> getAllAssets() - retrieve all assets with current prices and P/L
//   AssetResponseDto getAssetById(Long id) - get single asset or throw AssetNotFoundException
//   AssetResponseDto addAsset(AssetRequestDto dto) - create new asset from DTO
//   void deleteAsset(Long id) - remove asset from portfolio
//   List<AssetResponseDto> filterAssets(String ticker, AssetType type, LocalDate from, LocalDate to)
//     - query with optional filters
// Each returned asset should have currentPrice and profitLoss calculated
//
// IMPORTS NEEDED:
// import org.springframework.stereotype.Service;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.transaction.annotation.Transactional;
// import com.training.FAMPortfolioManager.repository.AssetRepository;
// import com.training.FAMPortfolioManager.model.Asset;
// import com.training.FAMPortfolioManager.model.AssetType;
// import com.training.FAMPortfolioManager.dto.AssetRequestDto;
// import com.training.FAMPortfolioManager.dto.AssetResponseDto;
// import com.training.FAMPortfolioManager.exception.AssetNotFoundException;
// import java.util.List;
// import java.time.LocalDate;
// import java.util.stream.Collectors;


// This service class contains the business logic for managing assets in the portfolio. 
// It interacts with the AssetRepository to perform CRUD operations and uses the PriceService to fetch current

@Service
public class AssetService {

    // Logger for logging warnings and errors, especially when price retrieval fails
    private static final Logger LOGGER = Logger.getLogger(AssetService.class.getName());

    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    private final PriceService priceService;

    public AssetService(AssetRepository assetRepository, PortfolioRepository portfolioRepository, PriceService priceService) {
        this.assetRepository = assetRepository;
        this.portfolioRepository = portfolioRepository;
        this.priceService = priceService;
    }

    // CREATE
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

    // READ ALL BY PORTFOLIO
    public List<AssetResponseDto> getAssetsByPortfolio(Long portfolioId) {

        List<Asset> assets = assetRepository.findByPortfolioId(portfolioId);

        return assets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // READ ONE
    public AssetResponseDto getAssetById(Long assetId) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        return mapToResponse(asset);
    }

    // UPDATE
    public AssetResponseDto updateAsset(Long assetId, AssetRequestDto request) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setCompanyName(request.getCompanyName());
        asset.setTicker(request.getTicker());
        asset.setQuantity(request.getQuantity());
        asset.setPurchasePrice(request.getPurchasePrice());
        asset.setDatePurchased(request.getPurchaseDate());

        if (!asset.getPortfolio().getId().equals(request.getPortfolioId())) {
            Portfolio newPortfolio = portfolioRepository.findById(request.getPortfolioId())
                    .orElseThrow(() -> new RuntimeException("Portfolio not found"));
            asset.setPortfolio(newPortfolio);
        }

        Asset updated = assetRepository.save(asset);

        return mapToResponse(updated);
    }

    // DELETE
    public void deleteAsset(Long assetId) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        assetRepository.delete(asset);
    }

    // MAPPING
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

    private double resolveCurrentPrice(Asset asset) {
        try {
            return round(priceService.getCurrentPrice(asset.getTicker()));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Falling back to purchase price for ticker " + asset.getTicker(), ex);
            return round(asset.getPurchasePrice());
        }
    }

    private double round(double value) {
        return new java.math.BigDecimal(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
