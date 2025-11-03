package com.stockmatch.stock.dto;

import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.SecurityType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecurityRequest {

    private String ticker;      // 종목 코드
    private String name;        // 종목명
    private Market market;      // 시장
    private Exchange exchange;  // 거래소
    private Currency currency;  // 통화
    private SecurityType type;  // 주식/ETF
}
