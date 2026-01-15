import { WatchlistItem } from "./watchlistItem";

/**
 * 관심종목 폴더
 */
export interface Watchlist {
    id: number;
    name: string;
    items: WatchlistItem[];
}

/**
 * 새 관심종목 폴더 추가 요청
 */
export interface WatchlistCreateRequest {
    name: string;
}

/**
 * 관심종목 이름 변경 요청
 */
export interface WatchlistUpdateRequest {
    name: string;
}

/**
 * 관심종목 폴더 순서 변경 요청
 */
export interface WatchlistSortRequest {
    watchlistIds: number[];
}