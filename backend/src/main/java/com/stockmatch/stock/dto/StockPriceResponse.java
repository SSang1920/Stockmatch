package com.stockmatch.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponse {

    private Region region;          // KR, US
    private String ticker;          // 종목 코드
    private String name;            // 종목명
    private double currentPrice;    // 현재가
    private double prevClose;       // 전일 종가
    private double openPrice;       // 시가
    private double highPrice;       // 고가
    private double lowPrice;        // 저가
    private double changeRate;      // 등락률
}
