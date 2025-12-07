package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PortfolioDailyValuationResponse(

        LocalDate date,
        BigDecimal totalInvested,
        BigDecimal totalValue,
        BigDecimal totalPnl,
        BigDecimal totalRate,
        List<HoldingValuationResponse> holdings
) {
}
