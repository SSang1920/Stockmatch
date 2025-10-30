package com.stockmatch.stock.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinnhubQuoteResponse {

    private double c;   // 현재 주가
    private double h;   // 고가
    private double l;   // 저가
    private double o;   // 시가
    private double pc;  // 전일 종가
    private long t;     // 타임스탬프
}
