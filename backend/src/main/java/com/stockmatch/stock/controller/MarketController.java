package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.MarketOverviewResponse;
import com.stockmatch.stock.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MarketOverviewResponse>> getOverview() {
        MarketOverviewResponse result = marketService.getGlobalMarketOverview();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
