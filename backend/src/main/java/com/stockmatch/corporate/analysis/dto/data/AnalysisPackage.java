package com.stockmatch.corporate.analysis.dto.data;

import com.stockmatch.corporate.analysis.dto.component.BusinessPerformance;
import com.stockmatch.corporate.analysis.dto.component.FinancialHealth;
import com.stockmatch.corporate.analysis.dto.component.MarketMomentum;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalysisPackage {
    private UserContext user; //유저 투자 성향

    private TargetStockInfo targetStock;
    private BusinessPerformance businessPerformance; // 1단계 :수익성 / 성장성
    private MarketMomentum marketMomentum; // 2단계 : 시장 분위기 / 실적
    private FinancialHealth financialHealth; // 3단계 : 재무 안정성 / 현금흐름

    private List<MissingDataItem> missingData;

    @Getter
    @Builder
    public static class TargetStockInfo {
        private String ticker;
        private String name;
        private String market;
        private String sector;
    }


}
