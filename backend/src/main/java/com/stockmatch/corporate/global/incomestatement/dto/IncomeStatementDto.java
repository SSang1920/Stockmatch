package com.stockmatch.corporate.global.incomestatement.dto;

import com.stockmatch.corporate.common.dto.CacheableData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementDto implements CacheableData {

    private String symbol;
    private List<AnnualReports> annualReports;

    @Override
    @JsonIgnore
    public boolean isValidForCaching() {

        return symbol != null && annualReports != null && !annualReports.isEmpty();
    }

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
