package com.stockmatch.corporate.earnings.dto;


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
public class EarningsDto implements CacheableData {

    private String symbol;
    private List<AnnualEarning> annualEarnings;
    private List<QuarterlyEarning> quarterlyEarnings;

    @Override
    @JsonIgnore
    public boolean isValidForCaching() {
        boolean hasAnnual = annualEarnings != null && !annualEarnings.isEmpty();
        boolean hasQuarterly = quarterlyEarnings != null && !quarterlyEarnings.isEmpty();
        return symbol != null && (hasAnnual || hasQuarterly);
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnualEarning {
        private String fiscalDateEnding;
        private String reportedEPS;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterlyEarning {
        private String fiscalDateEnding;
        private String reportedDate;
        private String reportedEPS;
        private String estimatedEPS;
        private String surprise;
        private String surprisePercentage;
    }


}
