package com.training.FAMPortfolioManager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.training.FAMPortfolioManager.model.AssetMonthlyPrice;
import com.training.FAMPortfolioManager.repository.AssetMonthlyPriceRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Handles all communication with the Twelve Data API for stock prices.
//
// Two main responsibilities:
//   1. getCurrentPrice()          - fetches the latest price for a ticker with a 3-tier cache
//                                   (fresh → stale → failure) to avoid hammering the API.
//   2. ensureMonthlySeriesStored() - fetches a full monthly close-price history and saves it to
//                                   the DB so the performance charts don't need live API calls.
@Service
public class PriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceService.class);
    private static final String FRESH_PRICE_CACHE = "stockPrices";
    private static final String STALE_PRICE_CACHE = "stockPricesFallback";
    private static final String FAILURE_PRICE_CACHE = "stockPriceFailures";
    private static final String HISTORY_FAILURE_CACHE = "stockHistoryFailures";

    @Value("${twelvedata.api.key}")
    private String apiKey;

    @Value("${twelvedata.price.url}")
    private String priceUrl;

    @Value("${twelvedata.time-series.url}")
    private String timeSeriesUrl;

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final AssetMonthlyPriceRepository assetMonthlyPriceRepository;
    
    public PriceService(RestTemplate restTemplate,
            CacheManager cacheManager,
            AssetMonthlyPriceRepository assetMonthlyPriceRepository) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
        this.assetMonthlyPriceRepository = assetMonthlyPriceRepository;
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
        String url = UriComponentsBuilder.fromUriString(priceUrl)
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

    public void ensureMonthlySeriesStored(String symbol, LocalDate fromDate, LocalDate toDate) {
        String normalizedSymbol = normalizeSymbol(symbol);
        LocalDate startMonth = YearMonth.from(fromDate).atDay(1);
        LocalDate endMonth = YearMonth.from(toDate).atDay(1);

        if (startMonth.isAfter(endMonth)) {
            return;
        }

        if (hasStoredCoverage(normalizedSymbol, startMonth, endMonth)) {
            return;
        }

        String historyFailureKey = "history:" + normalizedSymbol;
        String cachedFailure = getCachedHistoryFailure(historyFailureKey);
        if (cachedFailure != null) {
            LOGGER.debug("Skipping Twelve Data history call for {} due to cached provider failure: {}", normalizedSymbol, cachedFailure);
            return;
        }

        try {
            fetchAndPersistMonthlySeries(normalizedSymbol, startMonth, endMonth);
            clearCachedHistoryFailure(historyFailureKey);
        } catch (RuntimeException ex) {
            cacheHistoryFailure(historyFailureKey, ex.getMessage());
            LOGGER.warn("Could not hydrate monthly history for {}: {}", normalizedSymbol, ex.getMessage());
        }
    }

    public Double getMonthlyClosePrice(String symbol, LocalDate monthDate) {
        String normalizedSymbol = normalizeSymbol(symbol);
        LocalDate month = YearMonth.from(monthDate).atDay(1);
        AssetMonthlyPrice row = assetMonthlyPriceRepository
                .findTopByTickerAndPriceDateLessThanEqualOrderByPriceDateDesc(normalizedSymbol, month);
        return row == null ? null : row.getClosePrice();
    }

    private boolean hasStoredCoverage(String symbol, LocalDate startMonth, LocalDate endMonth) {
        AssetMonthlyPrice earliest = assetMonthlyPriceRepository.findTopByTickerOrderByPriceDateAsc(symbol);
        AssetMonthlyPrice latest = assetMonthlyPriceRepository.findTopByTickerOrderByPriceDateDesc(symbol);
        if (earliest == null || latest == null) {
            return false;
        }

        LocalDate requiredLatest = endMonth.isAfter(LocalDate.now().withDayOfMonth(1))
                ? LocalDate.now().withDayOfMonth(1)
                : endMonth;

        return !earliest.getPriceDate().isAfter(startMonth)
                && !latest.getPriceDate().isBefore(requiredLatest.minusMonths(1));
    }

    private void fetchAndPersistMonthlySeries(String symbol, LocalDate startMonth, LocalDate endMonth) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_KEY".equalsIgnoreCase(apiKey)) {
            throw new IllegalStateException("Twelve Data API key is not configured. Set twelvedata.api.key in application.properties.");
        }

        String url = UriComponentsBuilder.fromUriString(timeSeriesUrl)
                .queryParam("symbol", symbol)
                .queryParam("interval", "1month")
                .queryParam("start_date", startMonth)
                .queryParam("end_date", endMonth.plusMonths(1))
                .queryParam("order", "ASC")
                .queryParam("apikey", apiKey)
                .toUriString();

        Map<?, ?> response;
        try {
            response = restTemplate.getForObject(url, Map.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to reach Twelve Data time series endpoint for symbol: " + symbol, ex);
        }

        if (response == null || response.isEmpty()) {
            throw new IllegalStateException("No response from Twelve Data time series endpoint for symbol: " + symbol);
        }

        Object status = response.get("status");
        if (status != null && "error".equalsIgnoreCase(status.toString())) {
            Object message = response.get("message");
            String details = message == null ? "Unknown Twelve Data time-series error." : message.toString();
            throw new IllegalStateException("Twelve Data error for symbol " + symbol + ": " + details);
        }

        Object valuesObj = response.get("values");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            throw new IllegalStateException("No monthly values returned by Twelve Data for symbol: " + symbol);
        }

        List<AssetMonthlyPrice> toSave = new ArrayList<>();

        for (Object rowObj : values) {
            if (!(rowObj instanceof Map<?, ?> row)) {
                continue;
            }

            Object dt = row.get("datetime");
            Object close = row.get("close");
            if (dt == null || close == null) {
                continue;
            }

            LocalDate rowDate = parseDate(dt.toString());
            LocalDate monthDate = YearMonth.from(rowDate).atDay(1);

            double closePrice;
            try {
                closePrice = Double.parseDouble(close.toString());
            } catch (NumberFormatException ex) {
                continue;
            }

            if (closePrice <= 0) {
                continue;
            }

            AssetMonthlyPrice existing = assetMonthlyPriceRepository
                    .findByTickerAndPriceDate(symbol, monthDate)
                    .orElse(null);

            if (existing == null) {
                AssetMonthlyPrice created = new AssetMonthlyPrice();
                created.setTicker(symbol);
                created.setPriceDate(monthDate);
                created.setClosePrice(closePrice);
                created.setFetchedAt(LocalDateTime.now());
                toSave.add(created);
            } else {
                existing.setClosePrice(closePrice);
                existing.setFetchedAt(LocalDateTime.now());
                toSave.add(existing);
            }
        }

        if (!toSave.isEmpty()) {
            assetMonthlyPriceRepository.saveAll(toSave);
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw.length() >= 10) {
            return LocalDate.parse(raw.substring(0, 10));
        }
        throw new IllegalArgumentException("Invalid date returned by provider: " + raw);
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

    private String getCachedHistoryFailure(String key) {
        Cache cache = cacheManager.getCache(HISTORY_FAILURE_CACHE);
        return cache == null ? null : cache.get(key, String.class);
    }

    private void cacheHistoryFailure(String key, String message) {
        Cache cache = cacheManager.getCache(HISTORY_FAILURE_CACHE);
        if (cache != null) {
            cache.put(key, message);
        }
    }

    private void clearCachedHistoryFailure(String key) {
        Cache cache = cacheManager.getCache(HISTORY_FAILURE_CACHE);
        if (cache != null) {
            cache.evict(key);
        }
    }
}

