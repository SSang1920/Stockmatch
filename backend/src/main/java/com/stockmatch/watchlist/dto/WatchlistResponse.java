package com.stockmatch.watchlist.dto;

import com.stockmatch.watchlist.domain.Watchlist;

import java.util.List;
import java.util.stream.Collectors;

public record WatchlistResponse(
        Long id,
        String name,
        List<WatchlistItemResponse> items
) {
    // Entity -> Dto 변환 메서드
    public static WatchlistResponse from(Watchlist watchlist) {
        return new WatchlistResponse(
                watchlist.getId(),
                watchlist.getName(),
                watchlist.getWatchlistItems().stream()
                        .map(WatchlistItemResponse::from)
                        .collect(Collectors.toList())
        );
    }
}
