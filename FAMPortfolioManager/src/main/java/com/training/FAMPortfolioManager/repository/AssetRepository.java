package com.training.FAMPortfolioManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import com.training.FAMPortfolioManager.model.Asset;
import java.util.List;
// import java.time.LocalDate;


// This repository interface provides data access methods for Asset entities.
// It extends JpaRepository, which provides basic CRUD operations and allows 
// for custom query methods based on method naming conventions.
public interface AssetRepository extends JpaRepository<Asset, Long>
{

    // Custom query methods to find assets by specific fields.
    List<Asset> findByTicker(String ticker);

    List<Asset> findByCompanyName(String companyName);
    List<Asset> findByPortfolioId(long portfolioId);
}
