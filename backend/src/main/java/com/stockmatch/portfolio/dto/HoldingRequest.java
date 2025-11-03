package com.stockmatch.portfolio.dto;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 보유 종목 추가 요청 DTO
 */
@Getter
public class HoldingRequest {

    private String ticker;          // 종목 코드
    private BigDecimal quantity;    // 보유 수량
    private BigDecimal avgPrice;    // 평균 단가
}
