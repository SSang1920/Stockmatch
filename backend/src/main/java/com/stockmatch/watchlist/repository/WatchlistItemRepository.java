package com.stockmatch.watchlist.repository;

import com.stockmatch.watchlist.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    // 특정 폴더의 모든 아이템 조회
    List<WatchlistItem> findAllByWatchlistIdOrderByOrderNoAsc(Long watchlistId);

    // 새 아이템 만들 때 가장 마지막 순서 번호 가져오기
    @Query("SELECT MAX(wi.orderNo) FROM WatchlistItem wi WHERE wi.watchlist.id = :watchlistId")
    Integer findMaxOrderNoByWatchlistId(@Param("watchlistId") Long watchlistId);

    // 아이템 조회
    Optional<WatchlistItem> findByIdAndWatchlistIdAndWatchlist_UserId(Long itemId, Long watchlistId, Long userId);

    // 아이템 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WatchlistItem wi WHERE wi.id = :itemId AND wi.watchlist.id = :watchlistId AND wi.watchlist.user.id = :userId")
    int deleteByWatchlistItem(@Param("itemId") Long itemId,
                               @Param("watchlistId") Long watchlistId,
                               @Param("userId") Long userId);
}
