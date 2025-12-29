package com.stockmatch.stock.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MarketOverviewResponse(
        IndexInfo kospi,
        IndexInfo nasdaq,
        IndexInfo sp500,
        ExchangeRateInfo usdKrw,
        String lastUpdateTime
) {

    /**
     * 지수 정보 내부 레코드
     */
    @Builder
    public record IndexInfo(
            String name,
            BigDecimal price,       // 현재가
            BigDecimal change,      // 전일비
            BigDecimal changeRate   // 등락률
    ) {}

    /**
     * 환율 정보 내부 레코드
     */
    @Builder
    public record ExchangeRateInfo(
            String name,
            BigDecimal rate,        // 환율
            BigDecimal change,      // 전일비
            BigDecimal changeRate   // 등락률
    ) {}
}
