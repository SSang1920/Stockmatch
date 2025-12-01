package com.stockmatch.stock.dto;

import com.stockmatch.stock.domain.DailyPrice;

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

    public static DailyPriceResponse from(DailyPrice entity) {
        return new DailyPriceResponse(
                entity.getDate(),
                entity.getOpenPrice(),
                entity.getClosePrice(),
                entity.getHighPrice(),
                entity.getLowPrice(),
                entity.getVolume()
        );
    }
}
