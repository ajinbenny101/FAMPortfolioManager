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

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private LocalDateTime createdDate;

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