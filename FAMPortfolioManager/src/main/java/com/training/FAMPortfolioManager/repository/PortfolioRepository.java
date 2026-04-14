package com.training.FAMPortfolioManager.repository;

// PortfolioRepository - data access for Portfolio entities
// Extend JpaRepository<Portfolio, Long>
// Inherits findById() and other basic CRUD operations
// Optional custom methods:
//   Optional<Portfolio> findFirst() - get first (and typically only) portfolio
//   or: @Query("SELECT p FROM Portfolio p LIMIT 1") Portfolio findFirstPortfolio();
// Consider caching with @Cacheable for performance optimization
//
// IMPORTS NEEDED:
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Repository;

import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.Portfolio;

import java.util.List;
// import java.util.Optional;
public interface PortfolioRepository extends JpaRepository<Portfolio, Long>
{
        List<Asset> findByPortfolioId(long portfolioId);

    // @Cacheable("portfolio")
    // Optional<Portfolio> findFirstBy();

}