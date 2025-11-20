package com.stockmatch.corporate.earnings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsDto {

    private String symbol;
    private List<AnnualEarning> annualEarnings;
    private List<QuarterlyEarning> quarterlyEarnings;

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
