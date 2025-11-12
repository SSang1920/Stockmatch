package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioValuationResponse(

        Long portfolioId,
        BigDecimal totalInvested,       // 총매입금액
        BigDecimal totalValue,          // 총평가금액
        BigDecimal totalPnlAmount,      // 총손익
        BigDecimal totalPnlRate,        // 총수익률
        List<HoldingValuationResponse> holdings
) { }
