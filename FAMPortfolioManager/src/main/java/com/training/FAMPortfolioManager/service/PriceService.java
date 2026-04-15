package com.training.FAMPortfolioManager.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Service
public class PriceService {

   
    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    
    public PriceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @Cacheable(value = "stockPrices", key = "#symbol")
    public double getCurrentPrice(String symbol) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_KEY".equalsIgnoreCase(apiKey)) {
            throw new IllegalStateException("Alpha Vantage API key is not configured. Set alphavantage.api.key in application.properties.");
        }

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("function", "GLOBAL_QUOTE")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || response.isEmpty()) {
            throw new IllegalStateException("No response from Alpha Vantage for symbol: " + symbol);
        }

        if (response.containsKey("Note")) {
            throw new IllegalStateException("Alpha Vantage rate limit reached. Please retry shortly.");
        }

        Object quoteObject = response.get("Global Quote");
        if (!(quoteObject instanceof Map<?, ?> quote)) {
            throw new IllegalStateException("Missing Global Quote in Alpha Vantage response for symbol: " + symbol);
        }

        Object priceValue = quote.get("05. price");
        if (priceValue == null) {
            throw new IllegalStateException("Missing price field in Alpha Vantage response for symbol: " + symbol);
        }
        try {
            return Double.parseDouble(priceValue.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid price value returned by Alpha Vantage for symbol: " + symbol, ex);
        }
    }
}

