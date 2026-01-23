package com.stockmatch.corporate.global.balancesheet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class BalancesheetDto implements CacheableData {

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

        private String reportedCurrency;
        private String totalAssets;
        private String totalLiabilities;
        private String totalShareholderEquity;
        private String totalCurrentAssets;
        private String totalCurrentLiabilities;
        private String cashAndCashEquivalentsAtCarryingValue;
        private String longTermDebt;

    }
}
