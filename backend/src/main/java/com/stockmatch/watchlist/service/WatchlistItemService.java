package com.stockmatch.watchlist.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import com.stockmatch.watchlist.domain.Watchlist;
import com.stockmatch.watchlist.domain.WatchlistItem;
import com.stockmatch.watchlist.dto.WatchlistAddItemRequest;
import com.stockmatch.watchlist.dto.WatchlistItemSortRequest;
import com.stockmatch.watchlist.dto.WatchlistItemUpdateRequest;
import com.stockmatch.watchlist.repository.WatchlistItemRepository;
import com.stockmatch.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistItemService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final SecurityRepository securityRepository;


    /**
     * 관심종목 추가
     */
    @Transactional
    public Long addItem(Long userId, Long watchlistId, WatchlistAddItemRequest request) {
        Watchlist watchlist = validateWatchlistOwner(userId, watchlistId);
        Security security = securityRepository.findByTicker(request.ticker())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 해당 폴더의 마지막 순서 구하기
        Integer maxOrder = watchlistItemRepository.findMaxOrderNoByWatchlistId(watchlistId);
        int nextOrder = (maxOrder == null) ? 1 : maxOrder + 1;

        WatchlistItem item = WatchlistItem.builder()
                .security(security)
                .memo(request.memo())
                .orderNo(nextOrder)
                .build();

        watchlist.addItem(item);

        return item.getId();
    }

    /**
     * 관심종목 순서 변경
     */
    @Transactional
    public void sortItems(Long userId, Long watchlistId, WatchlistItemSortRequest request) {
        Watchlist watchlist = validateWatchlistOwner(userId, watchlistId);

        // 해당 폴더의 아이템들 조회
        List<WatchlistItem> items = watchlistItemRepository.findAllByWatchlistIdOrderByOrderNoAsc(watchlistId);
        Map<Long, WatchlistItem> itemMap = items.stream()
                .collect(Collectors.toMap(WatchlistItem::getId, Function.identity()));

        // 순서 업데이트
        List<Long> newOrderIds = request.itemIds();
        for (int i = 0; i < newOrderIds.size(); i++) {
            WatchlistItem item = itemMap.get(newOrderIds.get(i));
            if (item != null) {
                item.updateOrderNo(i + 1);
            }
        }
    }

    /**
     * 관심종목 아이템 수정
     */
    @Transactional
    public void updateItem(Long userId, Long watchlistId, Long itemId, WatchlistItemUpdateRequest request) {
        WatchlistItem item = validateItemOwner(userId, watchlistId, itemId);
        item.updateMemo(request.memo());
    }

    /**
     * 관심종목 아이템 삭제
     */
    @Transactional
    public void deleteItem(Long userId, Long watchlistId, Long itemId) {
        int deletedCount = watchlistItemRepository.deleteByWatchlistItem(itemId, watchlistId, userId);

        if (deletedCount == 0) {
            throw new BusinessException(ErrorCode.WATCHLIST_ITEM_NOT_FOUND);
        }
    }


    /**
     * 관심종목 목록 검증 메서드
     */
    private Watchlist validateWatchlistOwner(Long userId, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_NOT_FOUND));

        if (!watchlist.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return watchlist;
    }

    /**
     * 아이템 주인 검증 메서드
     */
    private WatchlistItem validateItemOwner(Long userId, Long watchlistId , Long itemId) {
        return watchlistItemRepository.findByIdAndWatchlistIdAndWatchlist_UserId(itemId, watchlistId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_ITEM_NOT_FOUND));
    }
}
