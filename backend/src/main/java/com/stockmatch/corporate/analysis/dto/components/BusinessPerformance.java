package com.stockmatch.corporate.analysis.dto.components;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BusinessPerformance {
    private String asOfDate; // "yyyy-MM-dd"

    // 수익성( 단위 : 0.15 = 15% // 서비스 로직에서 처리)
    private Double operatingMarginRatio; // 영업이익률
    private Double netProfitMarginRatio; // 순이익률
    private Double roeRatio; //자가 자본 이익률

    //성장성 (단위 : 0.05 = 5% 성장)
    private Double revenueGrowthRate; // 전년 대비 매출 성장률
    private Double operatingIncomeGrowthRate; //전년 대비 영업이익 성장률




}
