package com.stockmatch.corporate.global.cashflow.dto;

import com.stockmatch.corporate.common.dto.CacheableData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CashflowDto implements CacheableData {

    private String symbol;
    private List<AnnualReport> annualReports;

    @Override
    @JsonIgnore
    public boolean isValidForCaching() {

        return symbol != null && annualReports != null && !annualReports.isEmpty();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnnualReport {

        private String operatingCashflow;
        private String capitalExpenditures;
        private String dividendPayout;
        private String cashflowFromFinancing;
        private String netIncome;

    }
}
