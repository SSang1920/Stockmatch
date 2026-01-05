package com.stockmatch.corporate.korea.finance.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.korea.finance.dto.DartFinancialRawResponse;
import com.stockmatch.corporate.korea.finance.service.KoreaFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/corporate/korea")
public class KoreaFinanceController {

    private final KoreaFinanceService koreaFinanceService;

    @GetMapping("/{symbol}/finance")
    public ResponseEntity<ApiResponse<DartFinancialRawResponse>> getFinancialData(
            @PathVariable("symbol") String symbol,
            @RequestParam String year,
            @RequestParam String reportCode) {

        var data = koreaFinanceService.getFinancialData(symbol,year,reportCode);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

}
