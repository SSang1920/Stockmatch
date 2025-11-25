package com.stockmatch.stock.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExternalDailyPriceClient {

    record DailyPriceItem(
            LocalDate date,
            BigDecimal openPrice,
            BigDecimal closePrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal volume
    ) { }

    /**
     * 특정 티커의 기간별 일별 시세를 외부 API에서 조회
     */
    List<DailyPriceItem> getDailyPrices(String ticker, LocalDate from, LocalDate to);
}
