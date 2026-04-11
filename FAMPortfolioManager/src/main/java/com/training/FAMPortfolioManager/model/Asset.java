package com.training.FAMPortfolioManager.model;

// Asset - JPA entity representing a single investment asset
// Annotate with @Entity and @Table(name = "assets")
// RELATIONSHIPS:
//   @ManyToOne(fetch = FetchType.LAZY) - relationship to Portfolio
//   @JoinColumn(name = "portfolio_id", nullable = false)
//   private Portfolio portfolio; // Back-reference to owning portfolio
//
// Fields: // done 
//   id (Long) - @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//   ticker (String) - stock/crypto ticker symbol
//   companyName (String) - name of the asset issuer
//   quantity (double) - number of units held
//   purchasePrice (double) - price per unit at purchase
//   purchaseDate (LocalDateTime) - when asset was purchased
//   assetType (AssetType) - @Enumerated(EnumType.STRING) - STOCK, BOND, CRYPTO, CASH
// 
// IMPORTS NEEDED:
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import java.time.LocalDateTime;
// import lombok.Getter;
// import lombok.Setter;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import com.training.FAMPortfolioManager.model.Portfolio;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String assetType;
    private String name;
    private LocalDateTime datePurchased;
    private String ticker;
    private double quantity;
    private double purchasePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;


    public Asset() {
    }

    public Asset(String assetType, String name, LocalDateTime datePurchased, String ticker) {
        this.assetType = assetType;
        this.name = name;
        this.datePurchased = datePurchased;
        this.ticker = ticker;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDatePurchased() {
        return datePurchased;
    }

    public void setDatePurchased(LocalDateTime datePurchased) {
        this.datePurchased = datePurchased;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
}