package com.training.FAMPortfolioManager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Unit tests for AssetService.
// Repository and PriceService dependencies are mocked via Mockito so tests run
// without a database or external API.
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private AssetService assetService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setName("Growth");
    }

    @Test
    void addAsset_createsAssetAndReturnsMappedResponse() {
        AssetRequestDto request = new AssetRequestDto(
                "AAPL", "Apple", 10, 100, LocalDateTime.now().minusDays(3), 1L);

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        when(priceService.getCurrentPrice("AAPL")).thenReturn(120.0);
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        AssetResponseDto response = assetService.addAsset(request);

        assertEquals(99L, response.getId());
        assertEquals("AAPL", response.getTicker());
        assertEquals(120.0, response.getCurrentPrice());
        assertEquals(1200.0, response.getMarketValue());
        assertEquals(200.0, response.getProfitLoss());
    }

    @Test
    void updateAsset_whenPortfolioChanged_movesAssetToNewPortfolio() {
        Portfolio newPortfolio = new Portfolio();
        newPortfolio.setId(2L);

        Asset existing = new Asset();
        existing.setId(10L);
        existing.setTicker("AAPL");
        existing.setCompanyName("Apple");
        existing.setQuantity(1);
        existing.setPurchasePrice(100);
        existing.setPortfolio(portfolio);

        AssetRequestDto request = new AssetRequestDto(
                "NVDA", "Nvidia", 3, 200, LocalDateTime.now().minusDays(10), 2L);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.findById(2L)).thenReturn(Optional.of(newPortfolio));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(priceService.getCurrentPrice("NVDA")).thenReturn(220.0);

        AssetResponseDto response = assetService.updateAsset(10L, request);

        assertEquals("NVDA", response.getTicker());
        assertEquals(2L, existing.getPortfolio().getId());
    }

    @Test
    void getAssetById_whenMissing_throws() {
        when(assetRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.getAssetById(999L));
        assertTrue(ex.getMessage().contains("Asset not found"));
    }

    @Test
    void deleteAsset_whenFound_deletes() {
        Asset existing = new Asset();
        existing.setId(15L);
        when(assetRepository.findById(15L)).thenReturn(Optional.of(existing));

        assetService.deleteAsset(15L);

        verify(assetRepository).delete(existing);
    }
}
