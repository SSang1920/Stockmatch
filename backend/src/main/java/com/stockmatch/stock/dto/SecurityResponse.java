package com.stockmatch.stock.dto;

import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.SecurityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityResponse {

    private Long id;
    private String ticker;
    private String name;
    private Market market;
    private Exchange exchange;
    private Currency currency;
    private SecurityType type;
    private boolean delisted;
}
