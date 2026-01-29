package com.stockmatch.watchlist.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.watchlist.dto.*;
import com.stockmatch.watchlist.service.WatchlistItemService;
import com.stockmatch.watchlist.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final WatchlistItemService watchlistItemService;

    // ===== 관심종목 폴더 =====

    /**
     * 내 관심종목 폴더 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createWatchlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid WatchlistCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        Long watchlistId = watchlistService.createWatchlist(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(watchlistId));
    }

    /**
     * 내 관심종목 폴더 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchlistResponse>>> getMyWatchlists(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        List<WatchlistResponse> watchlists = watchlistService.getMyWatchlists(userId);
        return ResponseEntity.ok(ApiResponse.ok(watchlists));
    }

    /**
     * 내 관심종목 폴더 순서 변경
     */
    @PatchMapping("/sort")
    public ResponseEntity<ApiResponse<Void>> sortWatchlists(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody WatchlistSortRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        watchlistService.sortWatchlists(userId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 내 관심종목 폴더 이름 수정
     */
    @PatchMapping("/{watchlistId}")
    public ResponseEntity<ApiResponse<Void>> updateWatchlistName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId,
            @RequestBody @Valid WatchlistUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        watchlistService.updateWatchlist(userId, watchlistId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 내 관심종목 폴더 삭제
     */
    @DeleteMapping("/{watchlistId}")
    public ResponseEntity<ApiResponse<Void>> deleteWatchlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId
    ) {
        Long userId = userDetails.getUser().getId();
        watchlistService.deleteWatchlist(userId, watchlistId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // ===== 관심종목 아이템 =====

    /**
     * 관심종목 아이템 조회
     */
    @GetMapping("/{watchlistId}")
    public ResponseEntity<ApiResponse<WatchlistDetailResponse>> getWatchlistDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId
    ) {
        Long userId = userDetails.getUser().getId();
        WatchlistDetailResponse item = watchlistItemService.getWatchlistDetail(userId, watchlistId);
        return ResponseEntity.ok(ApiResponse.ok(item));
    }

    /**
     * 관심종목 아이템 추가
     */
    @PostMapping("/{watchlistId}/items")
    public ResponseEntity<ApiResponse<Long>> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId,
            @RequestBody @Valid WatchlistAddItemRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        Long itemId = watchlistItemService.addItem(userId, watchlistId, request);
        return ResponseEntity.ok(ApiResponse.ok(itemId));
    }

    /**
     * 관심종목 아이템 순서 변경
     */
    @PatchMapping("/{watchlistId}/items/sort")
    public ResponseEntity<ApiResponse<Void>> sortItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId,
            @RequestBody WatchlistItemSortRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        watchlistItemService.sortItems(userId, watchlistId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 관심종목 아이템 수정
     */
    @PatchMapping("/{watchlistId}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId,
            @PathVariable Long itemId,
            @RequestBody @Valid WatchlistItemUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        watchlistItemService.updateItem(userId, watchlistId, itemId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 관심종목 아이템 삭제
     */
    @DeleteMapping("/{watchlistId}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long watchlistId,
            @PathVariable Long itemId
    ) {
        Long userId = userDetails.getUser().getId();
        watchlistItemService.deleteItem(userId, watchlistId, itemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}

