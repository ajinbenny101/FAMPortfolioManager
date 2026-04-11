package com.training.FAMPortfolioManager.service;

// PriceService - external price data service
// Annotate with @Service
// DEPENDENCY INJECTION:
//   @Autowired or constructor injection of:
//     - RestTemplate (configure as @Bean in config)
//     - or WebClient (Spring WebFlux alternative)
//
// Methods:
//   double getCurrentPrice(String ticker) - calls Yahoo Finance API for latest price
//     Returns: double - current market price for the given ticker
//     API URL example: https://query1.finance.yahoo.com/v10/finance/quoteSummary/{ticker}?modules=price
//   double getPriceAtDate(String ticker, LocalDate date) - fetches historical price
//     Returns: double - historical price closest to the specified date
//     API URL with date range: https://query1.finance.yahoo.com/v7/finance/download/{ticker}?...
// Used by AssetService and PortfolioService to fetch market data
//
// IMPORTS NEEDED:
// import org.springframework.stereotype.Service;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.cache.annotation.Cacheable;
// import java.time.LocalDate;
// import java.util.Optional;
// import org.springframework.http.ResponseEntity;
// import org.json.JSONObject; // or Jackson ObjectMapper
// IMPORTS NEEDED:
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PriceService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Cacheable(value = "currentPrices", key = "#ticker")
    public double getCurrentPrice(String ticker) {
        try {
            String url = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/" + ticker + "?modules=price";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && objectMapper != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode priceNode = root.path("quoteSummary").path("result").get(0).path("price").path("regularMarketPrice").path("raw");

                if (!priceNode.isMissingNode()) {
                    return priceNode.asDouble();
                }
            }
        } catch (Exception e) {
            // Log error and return fallback price
            System.err.println("Error fetching price for " + ticker + ": " + e.getMessage());
        }

        // Fallback: return a mock price for development/testing
        return getMockPrice(ticker);
    }

    @Cacheable(value = "historicalPrices", key = "#ticker + '_' + #date")
    public double getPriceAtDate(String ticker, LocalDate date) {
        // For now, return current price as historical price
        // In a real implementation, this would call Yahoo Finance historical data API
        return getCurrentPrice(ticker);
    }

    private double getMockPrice(String ticker) {
        // Simple mock prices for development/testing
        switch (ticker.toUpperCase()) {
            case "AAPL": return 150.0;
            case "GOOGL": return 2800.0;
            case "MSFT": return 300.0;
            case "TSLA": return 200.0;
            case "AMZN": return 3200.0;
            default: return 100.0; // Default mock price
        }
    }
}
