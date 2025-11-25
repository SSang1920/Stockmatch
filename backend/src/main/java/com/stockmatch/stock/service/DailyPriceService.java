package com.stockmatch.stock.service;

import com.stockmatch.stock.dto.DailyPriceResponse;

import java.time.LocalDate;
import java.util.List;

public interface DailyPriceService {

    /**
     * 특정 종목의 기간별 일일 시세 조회
     */
    List<DailyPriceResponse> getDailyPrices(String ticker, LocalDate from, LocalDate to);
}
