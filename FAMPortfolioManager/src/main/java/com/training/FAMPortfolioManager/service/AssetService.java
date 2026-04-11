package com.training.FAMPortfolioManager.service;

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
// IMPORTS NEEDED:
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.AssetType;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.exception.AssetNotFoundException;
import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PriceService priceService;

    public List<AssetResponseDto> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    public AssetResponseDto getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));
        return convertToResponseDto(asset);
    }

    public AssetResponseDto addAsset(AssetRequestDto dto) {
        Portfolio portfolio = portfolioRepository.findFirstPortfolio()
                .orElseThrow(() -> new RuntimeException("No portfolio found"));

        Asset asset = new Asset();
        asset.setTicker(dto.getTicker());
        asset.setCompanyName(dto.getCompanyName());
        asset.setQuantity(dto.getQuantity());
        asset.setPurchasePrice(dto.getPurchasePrice());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setAssetType(dto.getAssetType());
        asset.setPortfolio(portfolio);

        Asset savedAsset = assetRepository.save(asset);
        return convertToResponseDto(savedAsset);
    }

    public AssetResponseDto updateAsset(Long id, AssetRequestDto dto) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(id));

        asset.setTicker(dto.getTicker());
        asset.setCompanyName(dto.getCompanyName());
        asset.setQuantity(dto.getQuantity());
        asset.setPurchasePrice(dto.getPurchasePrice());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setAssetType(dto.getAssetType());

        Asset updatedAsset = assetRepository.save(asset);
        return convertToResponseDto(updatedAsset);
    }

    public void deleteAsset(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new AssetNotFoundException(id);
        }
        assetRepository.deleteById(id);
    }

    public List<AssetResponseDto> filterAssets(String ticker, AssetType assetType,
                                             LocalDateTime fromDate, LocalDateTime toDate) {
        List<Asset> assets;

        if (ticker != null && assetType != null && fromDate != null && toDate != null) {
            assets = assetRepository.findByDateRange(fromDate, toDate).stream()
                    .filter(asset -> asset.getTicker().equals(ticker) && asset.getAssetType() == assetType)
                    .collect(Collectors.toList());
        } else if (ticker != null) {
            assets = assetRepository.findByTicker(ticker);
        } else if (assetType != null) {
            assets = assetRepository.findByAssetType(assetType);
        } else if (fromDate != null && toDate != null) {
            assets = assetRepository.findByDateRange(fromDate, toDate);
        } else {
            assets = assetRepository.findAll();
        }

        return assets.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    private AssetResponseDto convertToResponseDto(Asset asset) {
        double currentPrice = priceService.getCurrentPrice(asset.getTicker());
        double marketValue = asset.getQuantity() * currentPrice;
        double profitLoss = marketValue - (asset.getQuantity() * asset.getPurchasePrice());
        double profitLossPercent = (profitLoss / (asset.getQuantity() * asset.getPurchasePrice())) * 100;

        return new AssetResponseDto(
                asset.getId(),
                asset.getTicker(),
                asset.getCompanyName(),
                asset.getQuantity(),
                asset.getPurchasePrice(),
                asset.getPurchaseDate(),
                asset.getAssetType(),
                currentPrice,
                marketValue,
                profitLoss,
                profitLossPercent
        );
    }
}