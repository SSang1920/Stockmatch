package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;

public record HoldingValuationResponse(

        Long holdingId,
        String ticker,              // 종목코드
        String name,                // 종목명
        String krName,              // 한글 종목명
        String currency,            // 통화
        BigDecimal quantity,        // 보유수량
        BigDecimal avgPrice,        // 매입단가
        BigDecimal currentPrice,    // 현재가
        BigDecimal invested,        // 매입금액(단가 * 수량)
        BigDecimal value,           // 평가금액(현재가 * 수량)
        BigDecimal pnlAmount,       // 평가손익(평가 - 매입)
        BigDecimal pnlRate          // 수익률(평가/매입 - 1)
) {}



