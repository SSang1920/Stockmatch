package com.stockmatch.corporate.analysis.dto.data;

import com.stockmatch.portfolio.dto.HoldingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class UserContext {
    private String investmentType;
    private Integer investmentScore;
    private List<AiPortfolioHoldingDto> currentHoldings;

    @Getter
    @Builder
    public static class AiPortfolioHoldingDto {
        private String ticker; // 종목 코드
        private String name; // 종목명

        //원본 데이터
        private BigDecimal quantity;
        private BigDecimal avgPrice; // 매입 평단가
        private BigDecimal currentPrice; // 현재 금액
        private String currency;
        //가공 데이터
        private Double amount; // 현재가 기준 평가금액
        private Double weightPct; // 현재가 기준 비중
    }

}

