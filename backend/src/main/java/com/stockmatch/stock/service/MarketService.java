package com.stockmatch.stock.service;

import com.stockmatch.exchangeRate.service.FxRateService;
import com.stockmatch.stock.client.kis.KisKorStockClient;
import com.stockmatch.stock.client.kis.KisUsStockClient;
import com.stockmatch.stock.dto.MarketOverviewResponse;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final KisKorStockClient kisKorStockClient;
    private final KisUsStockClient kisUsStockClient;
    private final FxRateService fxRateService;

    // KIS API 지수 코드
    private static final String CODE_KOSPI = "0001";      // 코스피
    private static final String CODE_NASDAQ = "COMP";    // 나스닥
    private static final String CODE_SP500 = "SPX";      // S&P 500

    public MarketOverviewResponse getGlobalMarketOverview() {
        // 코스피 조회
        StockPriceResponse kospi = kisKorStockClient.getKrIndexPrice(CODE_KOSPI);

        // 나스닥, S&P 500 조회
        StockPriceResponse nasdaq = kisUsStockClient.getUsIndexPrice(CODE_NASDAQ, "NASDAQ");
        StockPriceResponse sp500 = kisUsStockClient.getUsIndexPrice(CODE_SP500, "S&P 500");

        // 환율 조회
        BigDecimal usdRate = fxRateService.getLatestUsdToKrwRate(LocalDate.now());

        return MarketOverviewResponse.builder()
                .kospi(mapToIndex(kospi, "KOSPI"))
                .nasdaq(mapToIndex(nasdaq, "NASDAQ"))
                .sp500(mapToIndex(sp500, "S&P 500"))
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
                .change(src.getChangeAmount())        // 전일비
                .changeRate(src.getChangeRate())    // 등락률
                .build();
    }
}
