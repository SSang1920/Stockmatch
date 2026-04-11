package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioProfitStatsResponse(
        BigDecimal totalProfit,
        Double totalRate,
        BigDecimal annualProfit,
        Double annualRate,
        BigDecimal monthlyProfit,
        Double monthlyRate,
        BigDecimal dailyProfit,
        Double dailyRate,
        BigDecimal realizedProfit
) {
}
