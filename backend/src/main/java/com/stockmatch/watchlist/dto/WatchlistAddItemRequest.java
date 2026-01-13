package com.stockmatch.watchlist.dto;

public record WatchlistAddItemRequest(
        String ticker,
        String memo
) {
}
