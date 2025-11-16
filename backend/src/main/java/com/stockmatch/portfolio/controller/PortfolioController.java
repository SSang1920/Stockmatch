package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.HoldingRequest;
import com.stockmatch.portfolio.dto.HoldingResponse;
import com.stockmatch.portfolio.dto.PortfolioResponse;
import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
import com.stockmatch.portfolio.service.HoldingService;
import com.stockmatch.portfolio.service.PortfolioService;
import com.stockmatch.portfolio.service.PortfolioValuationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<HoldingResponse>> addMyHolding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid HoldingRequest request
    ) {
        var result = holdingService.addOrUpdateHolding(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/me/holdings")
    public ResponseEntity<ApiResponse<List<HoldingResponse>>> getMyHoldings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var result = holdingService.getMyHoldings(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("me/holdings/{holdingId}")
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
}
