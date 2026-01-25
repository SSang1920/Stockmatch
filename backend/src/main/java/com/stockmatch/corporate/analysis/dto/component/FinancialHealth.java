package com.stockmatch.corporate.analysis.dto.component;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinancialHealth {
    private String currency; // 통화 단위
    private UnitScale unit;

    // 부채 상환 능력
    private Double debtToEquityRatio; // 부채비율 (Total Liabilities / Total Shareholder Equity)
    private Double currentRatio; // 유동비율 (Current Assets / Current Liabilities)

    // 현금 창출 능력
    private Double operatingCashFlow; // 영업활동 현금흐름
    private Double capitalExpenditures; // 자본 지출
    private Double freeCashFlow; // 잉여현금흐름 (Operating Cash Flow - Capital Expenditures)
    private Boolean isCashFlowPositive; //현금흐름 양수여부 (실제 돈이 돌고 있는지)

    // 자금 구조
    private Double totalAssets; // 총자산
    private Double totalLiabilities; // 총부채
    private Double cashAndEquivalents; // 현금 및 현금성 자산

}
