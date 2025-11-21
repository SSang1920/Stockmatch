package com.stockmatch.corporate.cachflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CashflowDto {

    private String symbol;
    private List<AnnualReport> annualReports;

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
