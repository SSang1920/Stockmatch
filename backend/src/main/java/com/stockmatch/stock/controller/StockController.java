package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
