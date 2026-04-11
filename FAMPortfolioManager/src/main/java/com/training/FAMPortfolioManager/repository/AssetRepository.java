package com.training.FAMPortfolioManager.repository;

// AssetRepository - data access for Asset entities
// Extend JpaRepository<Asset, Long>
// Custom query methods:
//   findByTicker(String ticker) - find assets by stock ticker
//   findByAssetType(AssetType type) - find assets by type (STOCK, BOND, etc.)
//   @Query("SELECT a FROM Asset a WHERE a.purchaseDate BETWEEN :startDate AND :endDate")
//   List<Asset> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate)
//   - Date range filtering using purchaseDate between two LocalDate values
//
// IMPORTS NEEDED:
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.model.AssetType;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
public interface AssetRepository extends JpaRepository<Asset, Long> {

}