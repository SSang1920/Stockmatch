/**
 * 관심종목 아이템
 */
export interface WatchlistItem {
    id: number;
    ticker: string;
    securityName: string;
    market: string;
    memo: string | null;
    orderNo: number;
}

/**
 * 관심종목 추가 요청
 */
export interface WatchlistAddItemRequest {
    ticker: string;
    memo?: string;
}

/**
 * 관심종목 메모 수정 요청
 */
export interface WatchlistItemUpdateRequest {
    memo: string;
}

/**
 * 관심종목 아이템 순서 변경 요청
 */
export interface WatchlistItemSortRequest {
    itemIds: number[];
}