package com.training.FAMPortfolioManager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import com.training.FAMPortfolioManager.dto.PortfolioRequestDTO;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.Portfolio;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.repository.PortfolioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Unit tests for PortfolioService.
// Repository and PriceService dependencies are mocked via Mockito so tests run
// without a database or external API.
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void addPortfolio_savesAndMapsResponse() {
        PortfolioRequestDTO request = new PortfolioRequestDTO("Core", "Main portfolio");

        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> {
            Portfolio p = invocation.getArgument(0);
            p.setId(7L);
            return p;
        });

        PortfolioResponseDto dto = portfolioService.addPortfolio(request);

        assertEquals(7L, dto.getId());
        assertEquals("Core", dto.getName());
        assertEquals("Main portfolio", dto.getDescription());
    }

    @Test
    void updatePortfolio_updatesExisting() {
        Portfolio existing = new Portfolio();
        existing.setId(1L);
        existing.setName("Old");
        existing.setDescription("Old desc");

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioResponseDto dto = portfolioService.updatePortfolio(1L, new PortfolioRequestDTO("New", "New desc"));

        assertEquals("New", dto.getName());
        assertEquals("New desc", dto.getDescription());
    }

    @Test
    void deletePortfolio_whenExists_deletes() {
        Portfolio existing = new Portfolio();
        existing.setId(4L);
        when(portfolioRepository.findById(4L)).thenReturn(Optional.of(existing));

        portfolioService.deletePortfolio(4L);

        verify(portfolioRepository).delete(existing);
    }

    @Test
    void getOverallPerformance_groupsUniqueTickersAndBuildsSeries() {
        Asset a1 = asset("AAPL", 2, 100, LocalDateTime.now().minusMonths(2));
        Asset a2 = asset("aapl", 1, 120, LocalDateTime.now().minusMonths(1));
        Asset b1 = asset("MSFT", 3, 50, LocalDateTime.now().minusMonths(2));

        when(assetRepository.findAll()).thenReturn(List.of(a1, a2, b1));
        when(priceService.getMonthlyClosePrice(anyString(), any(LocalDate.class))).thenReturn(200.0);

        List<PerformanceDataPointDto> result = portfolioService.getOverallPerformance();

        YearMonth start = YearMonth.from(LocalDateTime.now().minusMonths(2).toLocalDate());
        YearMonth end = YearMonth.now();
        long expectedPoints = ChronoUnit.MONTHS.between(start, end) + 1;
        assertEquals(expectedPoints, result.size());
        assertTrue(result.getFirst().getTotalValue() > 0);

        // ensure monthly data hydration only once per unique normalized ticker
        verify(priceService, times(1)).ensureMonthlySeriesStored(eq("AAPL"), any(LocalDate.class), any(LocalDate.class));
        verify(priceService, times(1)).ensureMonthlySeriesStored(eq("MSFT"), any(LocalDate.class), any(LocalDate.class));
    }

    private Asset asset(String ticker, double qty, double purchasePrice, LocalDateTime purchasedAt) {
        Asset a = new Asset();
        a.setTicker(ticker);
        a.setQuantity(qty);
        a.setPurchasePrice(purchasePrice);
        a.setDatePurchased(purchasedAt);
        return a;
    }
}
