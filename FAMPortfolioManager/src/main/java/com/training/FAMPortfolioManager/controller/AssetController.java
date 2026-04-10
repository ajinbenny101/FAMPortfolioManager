package com.training.FAMPortfolioManager.controller;

import com.training.FAMPortfolioManager.repository.AssetRepository;
import com.training.FAMPortfolioManager.model.Asset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

// AssetController - REST endpoints for asset management
// CLASS ANNOTATIONS:
//   @RestController - marks as REST controller (returns JSON)
//   @RequestMapping("/api/assets") - base URL path for all methods
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
//     @GetMapping or @RequestMapping(method = RequestMethod.GET)
//   POST / - add new asset
//     @PostMapping with @RequestBody AssetRequestDto
//   DELETE /{id} - delete asset by id
//     @DeleteMapping("/{id}") with @PathVariable Long id
//   GET with query params ?ticker=, ?type=, ?from=, ?to= for filtering
//     @GetMapping with @RequestParam(required = false) parameters
//
// IMPORTS NEEDED:
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.beans.factory.annotation.Autowired;
// import com.training.FAMPortfolioManager.service.AssetService;
// import com.training.FAMPortfolioManager.dto.AssetRequestDto;
// import com.training.FAMPortfolioManager.dto.AssetResponseDto;
// import com.training.FAMPortfolioManager.model.AssetType;
// import java.time.LocalDate;
@RestController
@RequestMapping("/api/assets")
public class AssetController {

}