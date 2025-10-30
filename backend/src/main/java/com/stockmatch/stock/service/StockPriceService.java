package com.stockmatch.stock.service;

import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.infra.FinnhubClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final FinnhubClient finnhubClient;

    public FinnhubQuoteResponse getUsStockPrice(String symbol) {
        return finnhubClient.getQuote(symbol);
    }
}
