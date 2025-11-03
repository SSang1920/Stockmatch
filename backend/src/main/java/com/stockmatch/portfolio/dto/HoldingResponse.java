package com.stockmatch.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 보유 종목 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {

    private Long id;
    private String ticker;
    private String name;
    private BigDecimal quantity;
    private BigDecimal avgPrice;
    private String currency;
}
