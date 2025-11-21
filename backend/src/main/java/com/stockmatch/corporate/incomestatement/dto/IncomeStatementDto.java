package com.stockmatch.corporate.incomestatement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementDto {

    private String symbol;
    private List<AnnualReports> annualReports;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnualReports{
        private String totalRevenue;
        private String netIncome;
        private String operatingIncome;
        private String researchAndDevelopment;
        private String ebitda;



    }
}
