package com.training.FAMPortfolioManager.controller;

// imports given by Steve
import com.training.FAMPortfolioManager.PortfolioApplication;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.model.Asset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

// imports advised by the prompt and needed for the controller functionality
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import com.training.FAMPortfolioManager.service.AssetService;
import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.model.AssetType;
import java.time.LocalDate;


// AssetController - REST endpoints for asset management
// CLASS ANNOTATIONS:
//   @RestController - marks as REST controller (returns JSON) // done 
//   @RequestMapping("/api/assets") - base URL path for all methods // done
// FIELD ANNOTATIONS:
//   @Autowired or use constructor injection - inject AssetService
// METHOD ANNOTATIONS:
//   @GetMapping - GET request, no path = root 
//   @GetMapping("/{id}") - GET single item by ID
//   @PostMapping - POST request to create
//   @DeleteMapping("/{id}") - DELETE by ID
//   @RequestParam(required = false) - optional query parameters
//   @PathVariable Long id - extract {id} from URL
//   @RequestBody - parse request body to DTO
//
// Inject AssetService via @Autowired or constructor injection
// Endpoints:
//   GET / - retrieve all assets
//     @GetMapping or @RequestMapping(method = RequestMethod.GET) // done
//   POST / - add new asset
//     @PostMapping with @RequestBody AssetRequestDto 
//   DELETE /{id} - delete asset by id
//     @DeleteMapping("/{id}") with @PathVariable Long id
//   GET with query params ?ticker=, ?type=, ?from=, ?to= for filtering
//     @GetMapping with @RequestParam(required = false) parameters
//


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
// Get asset by ticker - GET /api/assets/{ticker}
    @GetMapping("/{ticker}")
    public ResponseEntity<Asset> getAssetByTicker(@PathVariable String ticker) {
        return assetRepository.findByTicker(ticker)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
// CreateAsset mapping - accepts AssetRequestDto, creates Asset entity, saves to DB, returns created asset with 201 status
    @PostMapping
    public Asset createAsset(@RequestBody AssetRequestDto assetRequest) {
        Asset asset = new Asset();
        asset.setName(assetRequest.getName());
        asset.setType(assetRequest.getType());
        asset.setDatePurchased(assetRequest.getDatePurchased());
        asset.setTicker(assetRequest.getTicker());

        Asset savedAsset = assetRepository.save(asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAsset).getBody();
    }

    @PutMapping("/{ticker}")
    public ResponseEntity<Asset> updateAsset(@PathVariable String ticker, @RequestBody AssetRequestDto assetRequest) {
        return assetRepository.findByTicker(ticker)
                .map(asset -> {
                    asset.setName(assetRequest.getName());
                    asset.setType(assetRequest.getType());
                    asset.setDatePurchased(assetRequest.getDatePurchased());
                    Asset updatedAsset = assetRepository.save(asset);
                    return ResponseEntity.ok(updatedAsset);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<Void> deleteAsset(@PathVariable String ticker) {
        return assetRepository.findByTicker(ticker)
                .map(asset -> {  // If asset is found, delete it and return 204 No Content
                    assetRepository.delete(asset);
                    System.out.println("Deleted asset with ticker: " + ticker);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> 
                System.out.println("Asset with ticker " + ticker + " not found for deletion."));
                ResponseEntity.notFound().build();
    }
}