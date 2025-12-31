package com.stockmatch.stock.service;

import com.stockmatch.stock.cache.PriceCacheService;
import com.stockmatch.stock.client.kis.KisKorStockClient;
import com.stockmatch.stock.client.kis.KisUsStockClient;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockPriceServiceTest {

    @Mock
    KisKorStockClient kisKorStockClient;

    @Mock
    KisUsStockClient kisUsStockClient;

    @Mock PriceCacheService priceCache;

    @InjectMocks StockPriceService stockPriceService;

    private StockPriceResponse us(String sym, BigDecimal price) {
        return StockPriceResponse.builder()
                .region(Region.US).ticker(sym).name(sym)
                .currentPrice(price).prevClose(price.subtract(BigDecimal.ONE))
                .openPrice(price.subtract(new BigDecimal("0.5"))).highPrice(price).lowPrice(price.subtract(new BigDecimal("2")))
                .changeRate(new BigDecimal("0.01"))
                .build();
    }

    private StockPriceResponse kr(String code, BigDecimal price) {
        return StockPriceResponse.builder()
                .region(Region.KR).ticker(code).name(code)
                .currentPrice(price).prevClose(price.subtract(BigDecimal.ONE))
                .openPrice(price.subtract(new BigDecimal("0.5"))).highPrice(price).lowPrice(price)
                .changeRate(new BigDecimal("0.01"))
                .build();
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(kisUsStockClient, kisKorStockClient, priceCache);
    }

    @Test
    void getUsStockPrice_usesCacheThenFetch() {
        when(priceCache.getOrLoad(eq("US"), eq("AAPL"), any()))
                .thenAnswer(inv -> {
                    return us("AAPL", new BigDecimal("100.0"));
                });

        var r = stockPriceService.getUsStockPrice("AAPL");
        assertThat(r.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("100.0"));
        verify(priceCache, times(1)).getOrLoad(eq("US"), eq("AAPL"), any());
        verifyNoInteractions(kisUsStockClient);
    }

    @Test
    void getKrStockPrice_usesCacheThenFetch() {
        when(priceCache.getOrLoad(eq("KR"), eq("005930"), any()))
                .thenReturn(kr("005930", new BigDecimal("70000.0")));

        var r = stockPriceService.getKrStockPrice("005930");
        assertThat(r.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("70000.0"));
        verify(priceCache, times(1)).getOrLoad(eq("KR"), eq("005930"), any());
        verifyNoInteractions(kisKorStockClient);
    }

    @Test
    void getUsStockPrices_bulkCacheMissesOnlyFetched() {
        List<String> symbols = List.of("AAPL", "MSFT");
        Map<String, StockPriceResponse> mocked =
                Map.of("AAPL", us("AAPL", new BigDecimal("100.0")), "MSFT", us("MSFT", new BigDecimal("200.0")));

        when(priceCache.getOrLoadBulk(eq("US"), eq(symbols), any()))
                .thenReturn(mocked);

        var result = stockPriceService.getUsStockPrices(symbols);
        assertThat(result).hasSize(2);
        assertThat(result.get("AAPL").getCurrentPrice()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(result.get("MSFT").getCurrentPrice()).isEqualByComparingTo(new BigDecimal("100.0"));

        verify(priceCache, times(1)).getOrLoadBulk(eq("US"), eq(symbols), any());
        verifyNoInteractions(kisUsStockClient);
    }
}
