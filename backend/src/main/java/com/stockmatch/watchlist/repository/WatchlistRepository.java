package com.stockmatch.watchlist.repository;

import com.stockmatch.watchlist.domain.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    // 유저의 관심목록 폴더 조회
    @Query("SELECT w FROM Watchlist w LEFT JOIN FETCH w.watchlistItems WHERE w.user.id = :userId ORDER BY w.orderNo")
    List<Watchlist> findAllByUserIdWithItems(@Param("userId") Long userId);

    // 새 폴더 만들 때 가장 마지막 순서 번호 가져오기
    @Query("SELECT MAX(w.orderNo) FROM Watchlist w WHERE w.user.id = :userId")
    Integer findMaxOrderNoByUserId(@Param("userId") Long userId);
}
