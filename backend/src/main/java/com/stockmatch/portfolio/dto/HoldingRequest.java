package com.stockmatch.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * 보유 종목 추가 요청 DTO
 */
public record HoldingRequest(

        @NotBlank(message = "종목 티커는 필수입니다.")
        String ticker,          // 종목 티커

        @NotNull(message = "보유 수량은 필수입니다.")
        @Positive(message = "보유 수량은 0보다 커야 합니다.")
        BigDecimal quantity,    // 보유 수량

        @NotNull(message = "평균 단가는 필수입니다.")
        @PositiveOrZero(message = "평균 단가는 0이상이어야 합니다.")
        BigDecimal avgPrice     // 평균 단가

) { }
