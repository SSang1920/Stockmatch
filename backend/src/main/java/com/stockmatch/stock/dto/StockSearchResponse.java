package com.stockmatch.stock.dto;

import com.stockmatch.stock.domain.Market;
import lombok.Builder;

@Builder
public record StockSearchResponse(

        Long id,
        String ticker,          // 티커
        String name,            // 한글 이름
        String englishName,     // 영어 이름
        Market market,          // 시장 (US, KR)
        String exchange         // 거래소 (KOSPI, KOSDAQ, NASDAQ, NYSE)
) {
}
