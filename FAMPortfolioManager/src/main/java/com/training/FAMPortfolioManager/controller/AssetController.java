package com.training.FAMPortfolioManager.controller;

import com.training.FAMPortfolioManager.PortfolioApplication;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.model.Asset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetRepository assetRepository;

    public AssetController(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @GetMapping
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    @PostMapping
    public Asset createAsset(@RequestBody Asset asset) {
        return assetRepository.save(asset);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Asset> updateAsset(@PathVariable Long id, @RequestBody Asset assetDetails) {
        return assetRepository.findById(id)
                .map(asset -> {
                    asset.setName(assetDetails.getName());
                    asset.setType(assetDetails.getType());
                    asset.setDatePurchased(assetDetails.getDatePurchased());
                    asset.setTicker(assetDetails.getTicker());
                    Asset updatedAsset = assetRepository.save(asset);
                    return ResponseEntity.ok(updatedAsset);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        return assetRepository.findById(id)
                .map(asset -> {
                    assetRepository.delete(asset);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}