package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.MarketOverviewResponse;
import com.stockmatch.stock.dto.StockTrendResponse;
import com.stockmatch.stock.service.MarketService;
import com.stockmatch.stock.service.StockRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final StockRankService stockRankService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MarketOverviewResponse>> getOverview() {
        MarketOverviewResponse result = marketService.getGlobalMarketOverview();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<Map<String, List<StockTrendResponse>>>> getMarketTrends() {
        Map<String, List<StockTrendResponse>> result = stockRankService.getMarketTrends();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
