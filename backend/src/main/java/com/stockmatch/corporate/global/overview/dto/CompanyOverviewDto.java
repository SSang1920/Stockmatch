package com.stockmatch.corporate.global.overview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockmatch.corporate.common.dto.CacheableData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewDto implements CacheableData {

    @Override
    @JsonIgnore
    public boolean isValidForCaching() {
        return symbol != null && !symbol.isEmpty() && name != null && !name.isEmpty();
    }

    @JsonProperty("Symbol")
    private String symbol;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Description")
    private String description;

    //국가 및 통화
    @JsonProperty("Currency")
    private String currency;
    @JsonProperty("Country")
    private String country;


    @JsonProperty("Sector")
    private String sector;
    @JsonProperty("Industry")
    private String industry;


    @JsonProperty("MarketCapitalization")
    private String marketCapitalization;

    // 가치 평가 및 수익성
    @JsonProperty("PERatio")
    private String peRatio;
    @JsonProperty("ForwardPE")
    private String forwardPe;
    @JsonProperty("PriceToSalesRatioTTM")
    private String psRatio;
    @JsonProperty("EPS")
    private String eps;
    @JsonProperty("DividendPerShare")
    private String dividendPerShare;
    @JsonProperty("DividendYield")
    private String dividendYield;
    @JsonProperty("BookValue")
    private String bookValue;

    // 현재 주가 파악
    @JsonProperty("52WeekHigh")
    private String week52High;
    @JsonProperty("52WeekLow")
    private String week52Low;
}
