package com.training.FAMPortfolioManager.controller;

import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import com.training.FAMPortfolioManager.service.AssetService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

// REST endpoints for managing assets.
// All routes are prefixed with /api/assets.
// Delegates all business logic to AssetService.
@RestController
@RequestMapping("/api/assets")
public class AssetController {

        private final AssetService assetService;

        public AssetController(AssetService assetService) {
                this.assetService = assetService;
        }

        // GET /api/assets?portfolioId={id} - returns all assets in a given portfolio
        @GetMapping
        public ResponseEntity<List<AssetResponseDto>> getAssetsByPortfolio(@RequestParam Long portfolioId) {
                return ResponseEntity.ok(assetService.getAssetsByPortfolio(portfolioId));
        }

        // GET /api/assets/{assetId} - returns a single asset by its ID
        @GetMapping("/{assetId}")
        public ResponseEntity<AssetResponseDto> getAssetById(@PathVariable Long assetId) {
                return ResponseEntity.ok(assetService.getAssetById(assetId));
        }

        // GET /api/assets/{assetId}/performance - returns the monthly value series for one asset
        // CacheControl.noStore() ensures the browser always fetches fresh data
        @GetMapping("/{assetId}/performance")
        public ResponseEntity<List<PerformanceDataPointDto>> getAssetPerformance(@PathVariable Long assetId) {
                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .body(assetService.getAssetPerformance(assetId));
        }

        // POST /api/assets - creates a new asset and returns it with HTTP 201
        @PostMapping
        public ResponseEntity<AssetResponseDto> createAsset(@Valid @RequestBody AssetRequestDto request) {
                return ResponseEntity.status(HttpStatus.CREATED).body(assetService.addAsset(request));
        }

        // PUT /api/assets/{assetId} - updates an existing asset
        @PutMapping("/{assetId}")
        public ResponseEntity<AssetResponseDto> updateAsset(
                        @PathVariable Long assetId,
                        @Valid @RequestBody AssetRequestDto request) {
                return ResponseEntity.ok(assetService.updateAsset(assetId, request));
        }

        // DELETE /api/assets/{assetId} - deletes an asset and returns HTTP 204 (no content)
        @DeleteMapping("/{assetId}")
        public ResponseEntity<Void> deleteAsset(@PathVariable Long assetId) {
                assetService.deleteAsset(assetId);
                return ResponseEntity.noContent().build();
        }
}
    

        //     public AssetController(AssetRepository assetRepository) {
        //         this.assetRepository = assetRepository;
        //     }

        //     @GetMapping
        //     public List<Asset> getAllAssets() {
        //         return assetRepository.findAll();
        //     }
        // // Get asset by ID - GET /api/assets/{id}
        //     @GetMapping("/{id}")
        //     public ResponseEntity<Asset> getAssetById(@PathVariable Long id) {
        //         return assetRepository.findById(id)
        //                 .map(ResponseEntity::ok)
        //                 .orElseGet(() -> ResponseEntity.notFound().build());
        //     }
        // // CreateAsset mapping - accepts AssetRequestDto, creates Asset entity, saves to DB, returns created asset with 201 status
        //     @PostMapping
        //     public Asset createAsset(@RequestBody AssetRequestDto assetRequest) {
        //         Asset asset = new Asset();
        //         asset.setName(assetRequest.getCompanyName());
        //         asset.setDatePurchased(assetRequest.getPurchaseDate());
        //         asset.setTicker(assetRequest.getTicker());

        //         Asset savedAsset = assetRepository.save(asset);
        //         return ResponseEntity.status(HttpStatus.CREATED).body(savedAsset).getBody();
        //     }

        //     @PutMapping("/{id}")
        //     public ResponseEntity<Asset> updateAsset(@PathVariable Long id, @RequestBody AssetRequestDto assetRequest) {
        //         return assetRepository.findById(id)
        //                 .map(asset -> {
        //                     asset.setName(assetRequest.getCompanyName());
        //                     asset.setDatePurchased(assetRequest.getPurchaseDate());
        //                     asset.setTicker(assetRequest.getTicker());
        //                     Asset updatedAsset = assetRepository.save(asset);
        //                     return ResponseEntity.ok(updatedAsset);
        //                 })
        //                 .orElseGet(() -> ResponseEntity.notFound().build());
        //     }

        //     @DeleteMapping("/{id}")
        //     public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        //         return assetRepository.findById(id)
        //                 .map(asset -> {  // If asset is found, delete it and return 204 No Content
        //                     assetRepository.delete(asset);
        //                     System.out.println("Deleted asset with ID: " + id);
        //                     return ResponseEntity.noContent().<Void>build();
        //                 })
        //                 .orElseGet(() -> {
        //                     System.out.println("Asset with ID " + id + " not found for deletion.");
        //                     return ResponseEntity.notFound().build();
        //                 });
        //     }
        // }