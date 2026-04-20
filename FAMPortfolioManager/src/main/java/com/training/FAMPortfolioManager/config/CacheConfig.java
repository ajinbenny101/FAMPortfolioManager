package com.training.FAMPortfolioManager.config;


import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching

// Configures four Caffeine in-memory caches used by PriceService:
//   stockPrices         - short-lived fresh prices (default 15 min) to avoid redundant API calls
//   stockPricesFallback - long-lived stale prices (default 24 h) used as a backup when the API is down
//   stockPriceFailures  - records failed API calls (default 15 min) so we don't retry them immediately
//   stockHistoryFailures - records failed history fetches (default 30 min)
public class CacheConfig {

    @Value("${pricing.cache.fresh-seconds:900}")
    private long freshCacheSeconds;

    @Value("${pricing.cache.stale-hours:24}")
    private long staleCacheHours;

    @Value("${pricing.cache.failure-minutes:15}")
    private long failureCacheMinutes;

        @Value("${pricing.cache.history-failure-minutes:30}")
        private long historyFailureCacheMinutes;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache freshPrices = new CaffeineCache("stockPrices",
                Caffeine.newBuilder()
                        .expireAfterWrite(freshCacheSeconds, TimeUnit.SECONDS)
                        .maximumSize(500)
                        .build());

        CaffeineCache stalePrices = new CaffeineCache("stockPricesFallback",
                Caffeine.newBuilder()
                        .expireAfterWrite(staleCacheHours, TimeUnit.HOURS)
                        .maximumSize(1000)
                        .build());

        CaffeineCache failures = new CaffeineCache("stockPriceFailures",
                Caffeine.newBuilder()
                        .expireAfterWrite(failureCacheMinutes, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .build());

        CaffeineCache historyFailures = new CaffeineCache("stockHistoryFailures",
                Caffeine.newBuilder()
                        .expireAfterWrite(historyFailureCacheMinutes, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(freshPrices, stalePrices, failures, historyFailures));
        return manager;
    }
} 
