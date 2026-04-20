package com.training.FAMPortfolioManager.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.service.PortfolioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;

    private PortfolioController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PortfolioController(portfolioService);
    }

    @Test
    void getAllPortfolios_returnsOk() {
        when(portfolioService.getAllPortfolios()).thenReturn(List.of(new PortfolioResponseDto()));

        ResponseEntity<List<PortfolioResponseDto>> response = controller.getAllPortfolios();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getOverallPerformance_returnsData() {
        when(portfolioService.getOverallPerformance()).thenReturn(List.of(new PerformanceDataPointDto(LocalDate.now(), 250.0)));

        ResponseEntity<List<PerformanceDataPointDto>> response = controller.getOverallPerformance();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deletePortfolio_returnsNoContent() {
        ResponseEntity<Void> response = controller.deletePortfolio(8L);

        verify(portfolioService).deletePortfolio(8L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
