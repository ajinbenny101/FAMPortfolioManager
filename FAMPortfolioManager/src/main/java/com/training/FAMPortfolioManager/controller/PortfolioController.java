package com.training.FAMPortfolioManager.controller;

import com.training.FAMPortfolioManager.dto.PortfolioRequestDTO;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import com.training.FAMPortfolioManager.service.PortfolioService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// REST endpoints for managing portfolios.
// All routes are prefixed with /api/portfolios.
// Delegates all business logic to PortfolioService.
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	// GET /api/portfolios - returns all portfolios
	@GetMapping
	public ResponseEntity<List<PortfolioResponseDto>> getAllPortfolios() {
		return ResponseEntity.ok(portfolioService.getAllPortfolios());
	}

	// GET /api/portfolios/{id} - returns a single portfolio by its ID
	@GetMapping("/{id}")
	public ResponseEntity<PortfolioResponseDto> getPortfolioById(@PathVariable Long id) {
		return ResponseEntity.ok(portfolioService.getPortfolioById(id));
	}

	// GET /api/portfolios/performance/overall - monthly total value across all portfolios combined
	// CacheControl.noStore() ensures the browser always fetches fresh data
	@GetMapping("/performance/overall")
	public ResponseEntity<List<PerformanceDataPointDto>> getOverallPerformance() {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(portfolioService.getOverallPerformance());
	}

	// GET /api/portfolios/{id}/performance - monthly value for one specific portfolio
	@GetMapping("/{id}/performance")
	public ResponseEntity<List<PerformanceDataPointDto>> getPortfolioPerformance(@PathVariable Long id) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(portfolioService.getPortfolioPerformance(id));
	}

	// POST /api/portfolios - creates a new portfolio and returns it with HTTP 201
	@PostMapping
	public ResponseEntity<PortfolioResponseDto> createPortfolio(@Valid @RequestBody PortfolioRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.addPortfolio(request));
	}

	// PUT /api/portfolios/{id} - updates an existing portfolio's name and description
	@PutMapping("/{id}")
	public ResponseEntity<PortfolioResponseDto> updatePortfolio(
			@PathVariable Long id,
			@Valid @RequestBody PortfolioRequestDTO request) {
		return ResponseEntity.ok(portfolioService.updatePortfolio(id, request));
	}

	// DELETE /api/portfolios/{id} - deletes a portfolio and returns HTTP 204 (no content)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
		portfolioService.deletePortfolio(id);
		return ResponseEntity.noContent().build();
	}

}