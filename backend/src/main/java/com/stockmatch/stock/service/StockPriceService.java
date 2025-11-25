package com.stockmatch.stock.service;

import com.stockmatch.stock.cache.PriceCacheService;
import com.stockmatch.stock.client.PriceClientRouter;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final PriceCacheService priceCache;
    private final PriceClientRouter router;

    /**
     * 미국 주식 단일 시세 조회: 캐시 우선 조회 + 미스 시 외부 호출
     */
    public StockPriceResponse getUsStockPrice(String symbol) {
        return priceCache.getOrLoad("US", symbol, () -> router.getRealtime("US", symbol));
    }

    /**
     * 국내 주식 단일 시세 조회: 캐시 우선 조회 + 미스 시 외부 호출
     */
    public StockPriceResponse getKrStockPrice(String code) {
        return priceCache.getOrLoad("KR", code, () -> router.getRealtime("KR", code));
    }

    /**
     * 미국 주식 다건 시세 조회: 캐시 우선 조회 + 미스 시 외부 호출
     */
    public Map<String, StockPriceResponse> getUsStockPrices(List<String> symbols) {
        return priceCache.getOrLoadBulk("US", symbols, miss -> {
                    Map<String, StockPriceResponse> fetched = new LinkedHashMap<>();
                    for (String s : miss) {
                        fetched.put(s, router.getRealtime("US", s));
                    }

                    return fetched;
                }
        );
    }
}
