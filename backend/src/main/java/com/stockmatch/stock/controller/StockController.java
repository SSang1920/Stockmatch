package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockPriceService stockPriceService;

    @GetMapping("/us/{symbol}")
    public ApiResponse<FinnhubQuoteResponse> getUsQuote(@PathVariable String symbol) {
        var data = stockPriceService.getUsStockPrice(symbol);
        return ApiResponse.ok(data);
    }

    @GetMapping("/us/batch")
    public ApiResponse<Map<String, FinnhubQuoteResponse>> getUsQuotes(@RequestBody List<String> symbols) {
        var data = stockPriceService.getUsStockPrices(symbols);
        return ApiResponse.ok(data);
    }
}
