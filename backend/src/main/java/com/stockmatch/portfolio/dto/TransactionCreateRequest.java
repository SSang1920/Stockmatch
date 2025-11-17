package com.stockmatch.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCreateRequest(

        Long securityId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        LocalDateTime tradeAt,
        String memo
) {
}
