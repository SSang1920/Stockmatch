package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.portfolio.dto.PortfolioResponse;
import com.stockmatch.portfolio.dto.PortfolioSummaryResponse;
import com.stockmatch.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("/me/ensure")
    public ApiResponse<PortfolioResponse> ensureMyPortfolio(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(portfolioService.ensureForUser(userId));
    }

    @GetMapping("/me/summary")
    public ApiResponse<PortfolioSummaryResponse> getMyPortfolioSummary(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(portfolioService.getSummaryForUser(userId));
    }
}
