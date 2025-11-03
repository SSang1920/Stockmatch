package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockPriceService stockPriceService;

    @GetMapping("/us/{symbol}")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getUsQuote(@PathVariable String symbol) {
        var data = stockPriceService.getUsStockPrice(symbol);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/us/batch")
    public ResponseEntity<ApiResponse<Map<String, StockPriceResponse>>> getUsQuotes(@RequestBody List<String> symbols) {
        var data = stockPriceService.getUsStockPrices(symbols);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/kr/{code}")
    public ResponseEntity<ApiResponse<Object>> getKrQuote(@PathVariable String code) {
        var data = stockPriceService.getKrStockPrice(code);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
