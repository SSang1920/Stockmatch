package com.stockmatch.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponse {

    private Region region;          // KR, US
    private String ticker;          // 종목 코드
    private String name;            // 종목명

    private BigDecimal currentPrice;    // 현재가
    private BigDecimal changeAmount;    // 전일대비
    private BigDecimal changeRate;      // 등락률

    private BigDecimal prevClose;       // 전일 종가
    private BigDecimal openPrice;       // 시가
    private BigDecimal highPrice;       // 고가
    private BigDecimal lowPrice;        // 저가
    private BigDecimal volume;          // 거래량
}
