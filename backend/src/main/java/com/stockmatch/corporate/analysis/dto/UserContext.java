package com.stockmatch.corporate.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserContext {
    private String investmentType;
    private Integer investmentScore;
    private List<HoldingDto> currentHoldings;

    @Getter
    @AllArgsConstructor
    public static class HoldingDto {
        private String symbol;
        private Double weightPct; // 비중
        private Double amountUsd; // 보유 금액
    }
}

