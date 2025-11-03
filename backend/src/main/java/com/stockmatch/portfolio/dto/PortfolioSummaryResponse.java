package com.stockmatch.portfolio.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PortfolioSummaryResponse {

    private double totalAsset;          // 총 평가금액
    private double totalProfitRate;     // 전체 수익률
    private List<HoldingSummary> holdings;

    @Getter
    @Builder
    public static class HoldingSummary {
        private String symbol;
        private String name;
        private double quantity;
        private double avgPrice;
        private double currentPrice;
        private double profitRate;
    }
}
