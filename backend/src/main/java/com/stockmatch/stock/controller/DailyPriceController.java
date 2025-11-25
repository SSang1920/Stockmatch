package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.DailyPriceResponse;
import com.stockmatch.stock.service.DailyPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class DailyPriceController {

    private final DailyPriceService dailyPriceService;

    @GetMapping("/{ticker}/daily-prices")
    public ResponseEntity<ApiResponse<List<DailyPriceResponse>>> getDailyPrices(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<DailyPriceResponse> result = dailyPriceService.getDailyPrices(ticker, from, to);
        return ResponseEntity.ok(ApiResponse.ok(result));

    }
}
