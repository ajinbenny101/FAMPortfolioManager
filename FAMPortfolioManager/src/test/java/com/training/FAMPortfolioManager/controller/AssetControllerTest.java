package com.training.FAMPortfolioManager.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.training.FAMPortfolioManager.dto.AssetRequestDto;
import com.training.FAMPortfolioManager.dto.AssetResponseDto;
import com.training.FAMPortfolioManager.dto.PerformanceDataPointDto;
import com.training.FAMPortfolioManager.service.AssetService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Unit tests for AssetController.
// AssetService is mocked so tests only verify that the controller:
//   - passes arguments through to the service correctly
//   - returns the right HTTP status codes
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    private AssetController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AssetController(assetService);
    }

    @Test
    void createAsset_returnsCreatedStatus() {
        AssetRequestDto req = new AssetRequestDto("AAPL", "Apple", 2, 100, LocalDateTime.now(), 1L);
        AssetResponseDto dto = new AssetResponseDto();
        dto.setId(10L);

        when(assetService.addAsset(req)).thenReturn(dto);

        ResponseEntity<AssetResponseDto> response = controller.createAsset(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
    }

    @Test
    void getAssetPerformance_returnsSeries() {
        when(assetService.getAssetPerformance(3L)).thenReturn(List.of(new PerformanceDataPointDto(LocalDate.now(), 120.0)));

        ResponseEntity<List<PerformanceDataPointDto>> response = controller.getAssetPerformance(3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deleteAsset_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteAsset(7L);

        verify(assetService).deleteAsset(7L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
