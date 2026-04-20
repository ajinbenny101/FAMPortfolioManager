package com.training.FAMPortfolioManager.model;

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



import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Represents a user's investment portfolio.
// A portfolio can contain many assets (one-to-many with Asset).
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

    // All assets belonging to this portfolio.
    // CascadeType.ALL means operations on Portfolio cascade to its assets.
    // orphanRemoval = true deletes assets that are removed from this list.
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