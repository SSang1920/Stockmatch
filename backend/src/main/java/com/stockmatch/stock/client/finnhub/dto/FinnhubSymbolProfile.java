package com.stockmatch.stock.client.finnhub.dto;

public record FinnhubSymbolProfile(

        String ticker,
        String name,
        String currency,
        String exchange
) {
}
