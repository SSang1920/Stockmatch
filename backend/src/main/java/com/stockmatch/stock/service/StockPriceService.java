package com.stockmatch.stock.service;

import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.infra.finnhub.FinnhubClient;
import com.stockmatch.stock.infra.kis.KisStockClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final FinnhubClient finnhubClient;
    private final KisStockClient kisStockClient;

    /**
     * 미국 주식 단일 시세 조회
     */
    public StockPriceResponse getUsStockPrice(String symbol) {
        return finnhubClient.getQuote(symbol);
    }

    /**
     * 미국 주식 다건 시세 조회
     */
    public Map<String, StockPriceResponse> getUsStockPrices(List<String> symbols) {
        Map<String, StockPriceResponse> result = new LinkedHashMap<>();
        for (String symbol : symbols) {
            StockPriceResponse quote = finnhubClient.getQuote(symbol);
            result.put(symbol, quote);
        }

        return result;
    }

    /**
     * 국내 주식 단일 시세 조회
     */
    public StockPriceResponse getKrStockPrice(String code) {
        return kisStockClient.getKoreaPrice(code);
    }
}
