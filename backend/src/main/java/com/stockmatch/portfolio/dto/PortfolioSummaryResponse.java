package com.stockmatch.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PortfolioSummaryResponse {

    private final Long portfolioId;
    private final Long userId;
    private final String baseCurrency;
    private final long holdingCount;
    private final long transactionCount;
}
