package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.service.DailyPriceSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/daily-price")
public class DailyPriceSyncController {

    private final DailyPriceSyncService dailyPriceSyncService;


    @PostMapping("/sync/{ticker}")
    public ResponseEntity<ApiResponse<Void>> syncDailyPrice(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        dailyPriceSyncService.syncDailyPrices(ticker, from, to);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
