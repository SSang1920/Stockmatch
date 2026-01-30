package com.stockmatch.watchlist.dto;

import com.stockmatch.watchlist.domain.Watchlist;

import java.util.List;
import java.util.stream.Collectors;

public record WatchlistDetailResponse(
        Long id,
        String name,
        List<WatchlistItemResponse> items
) {
    public static WatchlistDetailResponse from(Watchlist watchlist) {
        List<WatchlistItemResponse> itemDtos = watchlist.getWatchlistItems().stream()
                .map(WatchlistItemResponse::from)
                .collect(Collectors.toList());

        return new WatchlistDetailResponse(
                watchlist.getId(),
                watchlist.getName(),
                itemDtos
        );
    }
}
