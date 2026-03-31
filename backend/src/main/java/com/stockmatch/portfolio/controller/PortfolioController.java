package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.*;
import com.stockmatch.portfolio.service.HoldingService;
import com.stockmatch.portfolio.service.PortfolioService;
import com.stockmatch.portfolio.service.PortfolioValuationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioValuationService portfolioValuationService;
    private final HoldingService holdingService;

    @PostMapping("/me/ensure")
    public ResponseEntity<ApiResponse<PortfolioResponse>> ensureMyPortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var result = portfolioService.ensureForUser(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/me/holdings")
    public ResponseEntity<ApiResponse<HoldingResponse>> addHolding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid HoldingRequest request
    ) {
        HoldingResponse result = holdingService.addHolding(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/me/holdings/{holdingId}")
    public ResponseEntity<ApiResponse<HoldingResponse>> updateHolding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long holdingId,
            @RequestBody @Valid HoldingRequest request
    ) {
        HoldingResponse result = holdingService.updateHolding(userDetails.getUser().getId(), holdingId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/me/holdings")
    public ResponseEntity<ApiResponse<List<HoldingResponse>>> getMyHoldings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var result = holdingService.getMyHoldings(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/me/holdings/{holdingId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyHolding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long holdingId
    ) {
        holdingService.deleteMyHolding(userDetails.getUser().getId(), holdingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me/valuation")
    public ResponseEntity<ApiResponse<PortfolioValuationResponse>> getMyPortfolioValuation(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var portfolio = portfolioService.ensureForUser(userDetails.getUser().getId());
        var valuation = portfolioValuationService.calculate(portfolio.getPortfolioId());
        return ResponseEntity.ok(ApiResponse.ok(valuation));
    }

    @GetMapping("/me/valuation/daily")
    public ResponseEntity<ApiResponse<List<PortfolioDailySummaryResponse>>> getMyPortfolioDailyValuation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var portfolio = portfolioService.ensureForUser(userDetails.getUser().getId());
        var history = portfolioValuationService.calculateDailyHistory(portfolio.getPortfolioId(), from, to);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
