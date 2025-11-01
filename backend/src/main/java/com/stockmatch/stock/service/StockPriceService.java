package com.stockmatch.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.stock.dto.FinnhubQuoteResponse;
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
     * @param symbol 주식 심볼
     * @return
     */
    public FinnhubQuoteResponse getUsStockPrice(String symbol) {
        return finnhubClient.getQuote(symbol);
    }

    /**
     * 미국 주식 다건 시세 조회
     * @param symbols 주식 심볼 리스트
     * @return
     */
    public Map<String, FinnhubQuoteResponse> getUsStockPrices(List<String> symbols) {
        Map<String, FinnhubQuoteResponse> result = new LinkedHashMap<>();
        for (String symbol : symbols) {
            FinnhubQuoteResponse quote = finnhubClient.getQuote(symbol);
            result.put(symbol, quote);
        }

        return result;
    }

    public JsonNode getKrStockPrice(String code) {
        return kisStockClient.getKoreaPrice(code);
    }
}
