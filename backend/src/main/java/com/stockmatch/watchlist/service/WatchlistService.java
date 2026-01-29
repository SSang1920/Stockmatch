package com.stockmatch.watchlist.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import com.stockmatch.watchlist.domain.Watchlist;
import com.stockmatch.watchlist.dto.WatchlistCreateRequest;
import com.stockmatch.watchlist.dto.WatchlistResponse;
import com.stockmatch.watchlist.dto.WatchlistSortRequest;
import com.stockmatch.watchlist.dto.WatchlistUpdateRequest;
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
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;

    /**
     * 내 관심종목 폴더 목록 조회
     */
    public List<WatchlistResponse> getMyWatchlists(Long userId) {
        return watchlistRepository.findAllByUserIdWithItems(userId).stream()
                .map(WatchlistResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 관심종목 폴더 생성
     */
    @Transactional
    public Long createWatchlist(Long userId, WatchlistCreateRequest request) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 맨 뒤에 추가
        Integer maxOrder = watchlistRepository.findMaxOrderNoByUserId(userId);
        int nextOrder = (maxOrder == null) ? 1 : maxOrder + 1;

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .name(request.name())
                .orderNo(nextOrder)
                .build();

        return watchlistRepository.save(watchlist).getId();
    }

    /**
     * 폴더 순서 변경
     */
    @Transactional
    public void sortWatchlists(Long userId, WatchlistSortRequest request) {
        // 유저의 모든 폴더를 가져옴
        List<Watchlist> watchlists = watchlistRepository.findAllByUserIdWithItems(userId);
        Map<Long, Watchlist> watchlistMap = watchlists.stream()
                .collect(Collectors.toMap(Watchlist::getId, Function.identity()));

        List<Long> newOrderIds = request.watchlistIds();
        for (int i = 0; i < newOrderIds.size(); i++) {
            Watchlist watchlist = watchlistMap.get(newOrderIds.get(i));
            if (watchlist != null) {
                watchlist.updateOrderNo(i + 1);
            }
        }
    }

    /**
     * 관심종목 폴더 이름 수정
     */
    @Transactional
    public void updateWatchlist(Long userId, Long watchlistId, WatchlistUpdateRequest request) {
        Watchlist watchlist = validateWatchlistOwner(userId, watchlistId);
        watchlist.updateName(request.name());
    }

    /**
     * 관심종목 폴더 삭제
     */
    @Transactional
    public void deleteWatchlist(Long userId, Long watchlistId) {
        Watchlist watchlist = validateWatchlistOwner(userId, watchlistId);
        watchlistRepository.delete(watchlist);
    }

    /**
     * 관심종목 검증 편의 메서드
     */
    private Watchlist validateWatchlistOwner(Long userId, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_NOT_FOUND));

        if (!watchlist.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return watchlist;
    }
}
