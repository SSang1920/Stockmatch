package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.PortfolioResponse;
import com.stockmatch.portfolio.dto.PortfolioSummaryResponse;
import com.stockmatch.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("/me/ensure")
    public ResponseEntity<ApiResponse<PortfolioResponse>> ensureMyPortfolio(@AuthenticationPrincipal CustomUserDetails userDetails) {
        var result = portfolioService.ensureForUser(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getMyPortfolioSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        var result = portfolioService.getSummaryForUser(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
