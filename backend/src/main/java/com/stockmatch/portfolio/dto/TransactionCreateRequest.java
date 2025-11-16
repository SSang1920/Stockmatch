package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCreateRequest(

        Long SecurityId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        LocalDateTime TradeAt,
        String memo
) {
}
