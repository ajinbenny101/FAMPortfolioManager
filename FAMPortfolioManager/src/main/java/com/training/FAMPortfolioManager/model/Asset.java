package com.training.FAMPortfolioManager.model;

// Asset - JPA entity representing a single investment asset
// Annotate with @Entity and @Table(name = "assets")
// RELATIONSHIPS:
//   @ManyToOne(fetch = FetchType.LAZY) - relationship to Portfolio
//   @JoinColumn(name = "portfolio_id", nullable = false)
//   private Portfolio portfolio; // Back-reference to owning portfolio
//
// Fields:
//   id (Long) - @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//   ticker (String) - stock/crypto ticker symbol
//   companyName (String) - name of the asset issuer
//   quantity (double) - number of units held
//   purchasePrice (double) - price per unit at purchase
//   purchaseDate (LocalDateTime) - when asset was purchased
//   assetType (AssetType) - @Enumerated(EnumType.STRING) - STOCK, BOND, CRYPTO, CASH
// 
// IMPORTS NEEDED:
// import jakarta.persistence.Entity;
// import jakarta.persistence.Table;
// import jakarta.persistence.Id;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Enumerated;
// import jakarta.persistence.EnumType;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.FetchType;
// import java.time.LocalDateTime;
// import lombok.Getter;
// import lombok.Setter;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
public class Asset {

}