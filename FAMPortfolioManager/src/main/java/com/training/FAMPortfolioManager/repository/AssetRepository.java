package com.training.FAMPortfolioManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import com.training.FAMPortfolioManager.model.Asset;
import java.util.List;
// import java.time.LocalDate;
public interface AssetRepository extends JpaRepository<Asset, Long>
{
    List<Asset> findByTicker(String ticker);
    // @Query("SELECT a FROM Asset a WHERE a.purchaseDate BETWEEN :startDate AND :endDate")
    // List<Asset> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Asset> findByCompanyName(String companyName);
    List<Asset> findByPortfolioId(long portfolioId);
}
