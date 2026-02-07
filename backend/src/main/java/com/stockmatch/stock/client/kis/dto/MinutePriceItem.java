package com.stockmatch.stock.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MinutePriceItem(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime dateTime,
        BigDecimal open,
        BigDecimal close,
        BigDecimal high,
        BigDecimal low,
        BigDecimal volume
) {
}
