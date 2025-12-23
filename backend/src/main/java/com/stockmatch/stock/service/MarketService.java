package com.stockmatch.stock.service;

import com.stockmatch.exchangeRate.service.FxRateService;
import com.stockmatch.stock.dto.MarketOverviewResponse;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final StockPriceService stockPriceService;
    private final FxRateService fxRateService;

    // KIS API 기준 지수 코드
    private static final String SYMBOL_KOSPI = "0001";      // 코스피
    private static final String SYMBOL_NASDAQ = ".IXIC";    // 나스닥
    private static final String SYMBOL_SP500 = ".INX";      // S&P 500

    public MarketOverviewResponse getGlobalMarketOverview() {
        // 각 지수 및 환율 데이터 조회
        StockPriceResponse kospiData = stockPriceService.getKrStockPrice(SYMBOL_KOSPI);
        StockPriceResponse nasdaqData = stockPriceService.getUsStockPrice(SYMBOL_NASDAQ);
        StockPriceResponse sp500Data = stockPriceService.getUsStockPrice(SYMBOL_SP500);

        BigDecimal usdRate = fxRateService.getTodayUsdToKrwRate();

        return MarketOverviewResponse.builder()
                .kospi(mapToIndex(kospiData, "KOSPI"))
                .nasdaq(mapToIndex(nasdaqData, "NASDAQ"))
                .sp500(mapToIndex(sp500Data, "S&P 500"))
                .usdKrw(MarketOverviewResponse.ExchangeRateInfo.builder()
                        .rate(usdRate)
                        .build())
                .build();
    }

    /**
     * StockPriceResponse -> MarketOverviewResponse.IndexInfo 변환 헬퍼 메서드
     */
    private MarketOverviewResponse.IndexInfo mapToIndex(StockPriceResponse src, String name) {
        if (src == null) {
            // 데이터가 없을 경우 이름만 담아서 빈 객체 반환
            return MarketOverviewResponse.IndexInfo.builder()
                    .name(name)
                    .price(BigDecimal.ZERO)
                    .change(BigDecimal.ZERO)
                    .changeRate(BigDecimal.ZERO)
                    .build();
        }

        return MarketOverviewResponse.IndexInfo.builder()
                .name(name)
                .price(src.getCurrentPrice())       // 현재가
                .change(src.getChangeRate())        // 전일비
                .changeRate(src.getChangeRate())    // 등락률
                .build();
    }
}
