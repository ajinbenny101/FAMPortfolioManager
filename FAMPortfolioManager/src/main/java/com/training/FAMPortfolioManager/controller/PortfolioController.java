package com.training.FAMPortfolioManager.controller;

import com.training.FAMPortfolioManager.dto.PortfolioRequestDTO;
import com.training.FAMPortfolioManager.dto.PortfolioResponseDto;
import com.training.FAMPortfolioManager.service.PortfolioService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping
	public ResponseEntity<List<PortfolioResponseDto>> getAllPortfolios() {
		return ResponseEntity.ok(portfolioService.getAllPortfolios());
	}

	@GetMapping("/{id}")
	public ResponseEntity<PortfolioResponseDto> getPortfolioById(@PathVariable Long id) {
		return ResponseEntity.ok(portfolioService.getPortfolioById(id));
	}

	@PostMapping
	public ResponseEntity<PortfolioResponseDto> createPortfolio(@Valid @RequestBody PortfolioRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.addPortfolio(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<PortfolioResponseDto> updatePortfolio(
			@PathVariable Long id,
			@Valid @RequestBody PortfolioRequestDTO request) {
		return ResponseEntity.ok(portfolioService.updatePortfolio(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
		portfolioService.deletePortfolio(id);
		return ResponseEntity.noContent().build();
	}

}