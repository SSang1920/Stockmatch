package com.stockmatch.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPriceResponse(

        LocalDate date,
        BigDecimal openPrice,
        BigDecimal closePrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal volume
) {
}
