package com.training.FAMPortfolioManager.controller;

// imports given by Steve
import com.training.FAMPortfolioManager.PortfolioApplication;
import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.model.Asset;
import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.model.AssetType;

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

import jakarta.persistence.Inheritance;

import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.model.AssetType;
import java.time.LocalDate;
import java.time.LocalDateTime;


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

    @Autowired
    private AssetService assetService;
    private final AssetRepository assetRepository;

    
    @GetMapping
    public ResponseEntity<List<AssetResponseDto>> getAllAssets(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(23, 59, 59) : null;

        List<AssetResponseDto> assets = assetService.filterAssets(ticker, fromDateTime, toDateTime);
        return ResponseEntity.ok(assets);
    }

  @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDto> getAssetById(@PathVariable Long id) {
        AssetResponseDto asset = assetService.getAssetById(id);
        return ResponseEntity.ok(asset);
    }

    @PostMapping
    public ResponseEntity<AssetResponseDto> createAsset(@RequestBody AssetRequestDto assetRequest) {
        AssetResponseDto createdAsset = assetService.addAsset(assetRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAsset);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponseDto> updateAsset(@PathVariable Long id, @RequestBody AssetRequestDto assetRequest) {
        AssetResponseDto updatedAsset = assetService.updateAsset(id, assetRequest);
        return ResponseEntity.ok(updatedAsset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
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