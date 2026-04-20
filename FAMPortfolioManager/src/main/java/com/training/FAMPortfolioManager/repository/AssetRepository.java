package com.training.FAMPortfolioManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.training.FAMPortfolioManager.model.Asset;
import java.util.List;

// Data access for Asset entities.
// Extends JpaRepository for standard CRUD, plus a custom method to load all assets for a given portfolio.
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // Custom query methods to find assets by specific fields.
    List<Asset> findByTicker(String ticker);

    List<Asset> findByCompanyName(String companyName);
    List<Asset> findByPortfolioId(long portfolioId);
}
