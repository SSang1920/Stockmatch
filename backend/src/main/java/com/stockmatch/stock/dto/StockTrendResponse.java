package com.stockmatch.stock.dto;

public record StockTrendResponse(
        String ticker,      // 티커
        String name,        // 종목명
        String price,       // 현재가
        String change,      // 변동폭
        String changeRate,  // 등락률
        String market       // 시장
) {
}
