package com.stockmatch.stock.service;

import com.stockmatch.stock.cache.PriceCacheService;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.infra.finnhub.FinnhubClient;
import com.stockmatch.stock.infra.kis.KisStockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockPriceServiceTest {

    @Mock FinnhubClient finnhubClient;
    @Mock KisStockClient kisStockClient;
    @Mock PriceCacheService priceCache;

    @InjectMocks StockPriceService stockPriceService;

    private StockPriceResponse us(String sym, double price) {
        return StockPriceResponse.builder()
                .region(Region.US).ticker(sym).name(sym)
                .currentPrice(price).prevClose(price-1)
                .openPrice(price-0.5).highPrice(price+1).lowPrice(price-2)
                .changeRate(0.01)
                .build();
    }

    private StockPriceResponse kr(String code, double price) {
        return StockPriceResponse.builder()
                .region(Region.KR).ticker(code).name(code)
                .currentPrice(price).prevClose(price-1)
                .openPrice(price-0.5).highPrice(price+1).lowPrice(price-2)
                .changeRate(0.01)
                .build();
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(finnhubClient, kisStockClient, priceCache);
    }

    @Test
    void getUsStockPrice_usesCacheThenFetch() {
        when(priceCache.getOrLoad(eq("US"), eq("AAPL"), any()))
                .thenAnswer(inv -> {
                    return us("AAPL", 100.0);
                });

        var r = stockPriceService.getUsStockPrice("AAPL");
        assertThat(r.getCurrentPrice()).isEqualTo(100.0);
        verify(priceCache, times(1)).getOrLoad(eq("US"), eq("AAPL"), any());
        verifyNoInteractions(finnhubClient);
    }

    @Test
    void getKrStockPrice_usesCacheThenFetch() {
        when(priceCache.getOrLoad(eq("KR"), eq("005930"), any()))
                .thenReturn(kr("005930", 70000.0));

        var r = stockPriceService.getKrStockPrice("005930");
        assertThat(r.getCurrentPrice()).isEqualTo(70000.0);
        verify(priceCache, times(1)).getOrLoad(eq("KR"), eq("005930"), any());
        verifyNoInteractions(kisStockClient);
    }

    @Test
    void getUsStockPrices_bulkCacheMissesOnlyFetched() {
        List<String> symbols = List.of("AAPL", "MSFT");
        Map<String, StockPriceResponse> mocked =
                Map.of("AAPL", us("AAPL", 100.0), "MSFT", us("MSFT", 200.0));

        when(priceCache.getOrLoadBulk(eq("US"), eq(symbols), any()))
                .thenReturn(mocked);

        var result = stockPriceService.getUsStockPrices(symbols);
        assertThat(result).hasSize(2);
        assertThat(result.get("AAPL").getCurrentPrice()).isEqualTo(100.0);
        assertThat(result.get("MSFT").getCurrentPrice()).isEqualTo(200.0);

        verify(priceCache, times(1)).getOrLoadBulk(eq("US"), eq(symbols), any());
        verifyNoInteractions(finnhubClient);
    }
}
