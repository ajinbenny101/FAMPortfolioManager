package com.training.FAMPortfolioManager.controller;

// PortfolioController - REST endpoints for portfolio summary and performance
// Annotate with @RestController and @RequestMapping("/api/portfolio")
// Inject PortfolioService via @Autowired or constructor injection
// Endpoints:
//   GET /summary - return PortfolioResponseDto with total value, total return, and breakdown by type
//     @GetMapping("/summary")
//   GET /performance - return List<PerformanceDataPointDto> for line chart visualization
//     @GetMapping("/performance")
//
// IMPORTS NEEDED:
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.http.ResponseEntity;
// import org.springframework.beans.factory.annotation.Autowired;
// import com.training.FAMPortfolioManager.service.PortfolioService;
// import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
// import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
// import java.util.List;
// IMPORTS NEEDED:
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.training.FAMPortfolioManager.service.PortfolioService;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/summary")
    public ResponseEntity<PortfolioResponseDto> getSummary() {
        PortfolioResponseDto summary = portfolioService.getSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/performance")
    public ResponseEntity<List<PerformanceDataPointDto>> getPerformance() {
        List<PerformanceDataPointDto> performance = portfolioService.getPerformance();
        return ResponseEntity.ok(performance);
    }
}