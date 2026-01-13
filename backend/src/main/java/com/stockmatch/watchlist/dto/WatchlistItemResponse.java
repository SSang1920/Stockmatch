package com.stockmatch.watchlist.dto;

import com.stockmatch.watchlist.domain.WatchlistItem;

public record WatchlistItemResponse(
        Long id,
        String ticker,
        String securityName,
        String market,
        String memo,
        Integer orderNo
) {
    // Entity -> Dto 변환 메서드
    public static WatchlistItemResponse from(WatchlistItem item) {
        return new WatchlistItemResponse(
                item.getId(),
                item.getSecurity().getTicker(),
                item.getSecurity().getName(),
                item.getSecurity().getMarket().name(),
                item.getMemo(),
                item.getOrderNo()
        );
    }
}
