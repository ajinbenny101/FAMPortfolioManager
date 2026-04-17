package com.training.FAMPortfolioManager.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// PriceService - external price data service backed by Twelve Data.
// It retrieves latest prices and cooperates with the cache layer so repeated
// UI refreshes do not exhaust the provider unnecessarily.
@Service
public class PriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceService.class);
    private static final String FRESH_PRICE_CACHE = "stockPrices";
    private static final String STALE_PRICE_CACHE = "stockPricesFallback";
    private static final String FAILURE_PRICE_CACHE = "stockPriceFailures";

    @Value("${twelvedata.api.key}")
    private String apiKey;

    @Value("${twelvedata.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    
    public PriceService(RestTemplate restTemplate, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
    }
    // Fetches the current price for a given stock ticker symbol using Twelve Data.

    public double getCurrentPrice(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Double freshCachedPrice = getCachedPrice(FRESH_PRICE_CACHE, normalizedSymbol);
        if (freshCachedPrice != null) {
            return freshCachedPrice;
        }

        Double staleCachedPrice = getCachedPrice(STALE_PRICE_CACHE, normalizedSymbol);
        String cachedFailure = getCachedFailure(normalizedSymbol);
        if (cachedFailure != null) {
            if (staleCachedPrice != null) {
                cachePrice(FRESH_PRICE_CACHE, normalizedSymbol, staleCachedPrice);
                return staleCachedPrice;
            }
            throw new IllegalStateException(cachedFailure);
        }

        try {
            double latestPrice = fetchCurrentPrice(normalizedSymbol);
            cachePrice(FRESH_PRICE_CACHE, normalizedSymbol, latestPrice);
            cachePrice(STALE_PRICE_CACHE, normalizedSymbol, latestPrice);
            clearCachedFailure(normalizedSymbol);
            return latestPrice;
        } catch (RuntimeException ex) {
            cacheFailure(normalizedSymbol, ex.getMessage());
            if (staleCachedPrice != null) {
                LOGGER.warn("Using stale cached price for symbol {} after provider failure: {}", normalizedSymbol, ex.getMessage());
                cachePrice(FRESH_PRICE_CACHE, normalizedSymbol, staleCachedPrice);
                return staleCachedPrice;
            }

            throw ex;
        }
    }

    private double fetchCurrentPrice(String symbol) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_KEY".equalsIgnoreCase(apiKey)) {
            throw new IllegalStateException("Twelve Data API key is not configured. Set twelvedata.api.key in application.properties.");
        }

        // Twelve Data price endpoint returns a compact payload like:
        // { "price": "123.45" }
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .toUriString();

        Map<?, ?> response;
        try {
            response = restTemplate.getForObject(url, Map.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to reach Twelve Data for symbol: " + symbol, ex);
        }

        if (response == null || response.isEmpty()) {
            throw new IllegalStateException("No response from Twelve Data for symbol: " + symbol);
        }

        Object status = response.get("status");
        if (status != null && "error".equalsIgnoreCase(status.toString())) {
            Object message = response.get("message");
            String details = message == null ? "Unknown Twelve Data error." : message.toString();
            throw new IllegalStateException("Twelve Data error for symbol " + symbol + ": " + details);
        }

        if (response.containsKey("code") && !response.containsKey("price")) {
            Object message = response.get("message");
            String details = message == null ? "Unknown Twelve Data error." : message.toString();
            throw new IllegalStateException("Twelve Data rejected symbol " + symbol + ": " + details);
        }

        Object priceValue = response.get("price");
        if (priceValue == null || priceValue.toString().isBlank()) {
            throw new IllegalStateException("Missing price field in Twelve Data response for symbol: " + symbol);
        }

        try {
            double parsedPrice = Double.parseDouble(priceValue.toString());
            if (parsedPrice <= 0) {
                throw new IllegalStateException("Twelve Data returned a non-positive price for symbol: " + symbol);
            }
            return parsedPrice;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid price value returned by Twelve Data for symbol: " + symbol, ex);
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Ticker symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private Double getCachedPrice(String cacheName, String symbol) {
        Cache cache = cacheManager.getCache(cacheName);
        return cache == null ? null : cache.get(symbol, Double.class);
    }

    private void cachePrice(String cacheName, String symbol, double price) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(symbol, price);
        }
    }

    private String getCachedFailure(String symbol) {
        Cache cache = cacheManager.getCache(FAILURE_PRICE_CACHE);
        return cache == null ? null : cache.get(symbol, String.class);
    }

    private void cacheFailure(String symbol, String message) {
        Cache cache = cacheManager.getCache(FAILURE_PRICE_CACHE);
        if (cache != null) {
            cache.put(symbol, message);
        }
    }

    private void clearCachedFailure(String symbol) {
        Cache cache = cacheManager.getCache(FAILURE_PRICE_CACHE);
        if (cache != null) {
            cache.evict(symbol);
        }
    }
}

