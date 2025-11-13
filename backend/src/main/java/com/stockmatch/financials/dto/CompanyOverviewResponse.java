package com.stockmatch.financials.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewResponse {

    @JsonProperty("Symbol")
    private String symbol;
    @JsonProperty("AssetType")
    private String assetType;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("CIK")
    private String cik;
    @JsonProperty("Exchange")
    private String exchange;
    @JsonProperty("Currency")
    private String currency;
    @JsonProperty("Country")
    private String country;
    @JsonProperty("Sector")
    private String sector;
    @JsonProperty("Industry")
    private String industry;
    @JsonProperty("Address")
    private String address;
    @JsonProperty("MarketCapitalization")
    private String marketCapitalization;
    @JsonProperty("PERatio")
    private String peRatio;
    @JsonProperty("EPS")
    private String eps;
    @JsonProperty("DividendPerShare")
    private String dividendPerShare;
    @JsonProperty("DividendYield")
    private String dividendYield;
    @JsonProperty("BookValue")
    private String bookValue;
    @JsonProperty("52WeekHigh")
    private String week52High;
    @JsonProperty("52WeekLow")
    private String week52Low;
}
