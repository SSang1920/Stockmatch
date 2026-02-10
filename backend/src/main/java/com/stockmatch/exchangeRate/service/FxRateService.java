package com.stockmatch.exchangeRate.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FxRateService {

    /**
     * 특정 날짜 기준 USD -> KRW 환율 조회
     */
    BigDecimal getUsdToKrwRate(LocalDate date);

    /**
     * 특정 날짜 기준 가장 최근 환율
     */
    BigDecimal getLatestUsdToKrwRate(LocalDate date);

    void fetchAndSaveExchangeRateTrend();
}
