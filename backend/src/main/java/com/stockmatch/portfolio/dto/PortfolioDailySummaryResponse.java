package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioDailySummaryResponse(

        LocalDate date,             // 평가 기준 날짜
        BigDecimal totalInvested,   // 총 매입 금액
        BigDecimal totalValue,      // 총 평가 금액
        BigDecimal totalPnl,        // 총 손익
        BigDecimal totalRate        // 총 수익률
) {
}
