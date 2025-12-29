package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.dto.StockSearchResponse;
import com.stockmatch.stock.service.StockPriceService;
import com.stockmatch.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockPriceService stockPriceService;
    private final StockService stockService;

    @GetMapping("/us/{symbol}")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getUsQuote(@PathVariable String symbol) {
        var data = stockPriceService.getUsStockPrice(symbol);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/us/batch")
    public ResponseEntity<ApiResponse<Map<String, StockPriceResponse>>> getUsQuotes(@RequestParam List<String> symbols) {
        var data = stockPriceService.getUsStockPrices(symbols);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/kr/{code}")
    public ResponseEntity<ApiResponse<Object>> getKrQuote(@PathVariable String code) {
        var data = stockPriceService.getKrStockPrice(code);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StockSearchResponse>>> search(@RequestParam("q") String query) {
        List<StockSearchResponse> result = stockService.searchStocks(query);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
