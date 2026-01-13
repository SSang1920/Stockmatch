package com.stockmatch.watchlist.dto;

import java.util.List;

public record WatchlistSortRequest(
        List<Long> watchlistIds
) {
}
