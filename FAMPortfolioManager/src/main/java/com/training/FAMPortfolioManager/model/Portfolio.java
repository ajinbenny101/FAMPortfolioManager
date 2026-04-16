package com.training.FAMPortfolioManager.model;

// Portfolio - JPA entity representing the user's portfolio
// Annotate with @Entity and @Table(name = "portfolios")
// RELATIONSHIPS:
//   @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//   private List<Asset> assets; // Child assets owned by this portfolio
//   Location: Add this field after id, before name
//
// Fields:
//   id (Long) - @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//   name (String) - optional portfolio name
//   description (String) - optional portfolio description
//   assets (List<Asset>) - @OneToMany(mappedBy="portfolio") relationship with Asset entities
// Links all assets together as a single portfolio
//
// IMPORTS NEEDED:
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.List;
import java.util.ArrayList;



// IMPORTS NEEDED:

import java.time.LocalDateTime;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Portfolio - JPA entity representing the user's portfolio

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

// This entity class represents a user's investment portfolio, which can contain multiple assets. 
// The portfolio has a one-to-many relationship with the Asset entity, meaning that each portfolio 
// can have multiple assets, but each asset belongs to only one portfolio.
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String name;
    private String description;
    private LocalDateTime createdDate;

    // The @OneToMany annotation defines a one-to-many relationship between Portfolio and Asset.
    // The "mappedBy" attribute indicates that the "portfolio" field in the Asset class owns the relationship.
    // "cascade = CascadeType.ALL" means that any changes to the portfolio (like deletion) will cascade to its assets, ensuring data integrity.
    // "orphanRemoval = true" means that if an asset is removed from the portfolio's asset list, it will also be removed from the database, preventing orphaned records.
    // "fetch = FetchType.LAZY" means that the list of assets will not be loaded from the database until it is accessed, which can improve performance when loading portfolios without needing asset details immediately.

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Asset> assets = new ArrayList<>();

    // Helper methods for managing assets
    public void addAsset(Asset asset) {
        assets.add(asset);
        asset.setPortfolio(this);
    }

    public void removeAsset(Asset asset) {
        assets.remove(asset);
        asset.setPortfolio(null);
    }
}