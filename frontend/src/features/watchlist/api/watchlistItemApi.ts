import axios from '@/lib/axios';
import { WatchlistAddItemRequest, WatchlistItemSortRequest, WatchlistItemUpdateRequest } from "../types"
import { ApiResponse } from "@/types/common";

// 관심종목 추가
export const addWatchlistItem = async (watchlistId: number, ticker: string, memo?: string): Promise<number> => {
    const body: WatchlistAddItemRequest = { ticker, memo };
    const response = await axios.post<ApiResponse<number>>(`/watchlists/${watchlistId}/items`, body);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '종목 추가 실패');
    }

    return response.data.data;
};

// 관심종목 메모 수정
export const updateWatchlistItem = async (watchlistId: number, itemId: number, memo: string): Promise<void> => {
    const body: WatchlistItemUpdateRequest = { memo };
    const response = await axios.patch<ApiResponse<void>>(`/watchlists/${watchlistId}/items/${itemId}`, body);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '메모 수정 실패');
    }
};

// 관심종목 삭제
export const deleteWatchlistItem = async (watchlistId: number, itemId: number): Promise<void> => {
    const response = await axios.delete<ApiResponse<void>>(`/watchlists/${watchlistId}/items/${itemId}`);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '종목 삭제 실패');
    }
};

// 관심종목 순서 변경
export const sortWatchlistItems = async (watchlistId: number, itemIds: number[]): Promise<void> => {
    const body: WatchlistItemSortRequest = { itemIds };
    const response = await axios.patch<ApiResponse<void>>(`/watchlists/${watchlistId}/items/sort`, body);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '종목 순서 변경 실패');
    }
};