package com.stockmatch.corporate.overview.dto;

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
