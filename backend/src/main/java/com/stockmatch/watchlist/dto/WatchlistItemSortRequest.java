package com.stockmatch.watchlist.dto;

import java.util.List;

public record WatchlistItemSortRequest(
        List<Long> itemIds
) {
}
