package com.stockmatch.portfolio.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 보유 종목 응답 DTO
 */
@Builder
public record HoldingResponse(

        Long id,
        String ticker,
        String name,
        BigDecimal quantity,
        BigDecimal avgPrice,
        String currency
) {
}
