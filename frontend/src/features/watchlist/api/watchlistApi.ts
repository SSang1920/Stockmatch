import axios from "axios";
import { Watchlist, WatchlistCreateRequest, WatchlistSortRequest, WatchlistUpdateRequest } from "../types";
import { ApiResponse } from "@/types/common";

// 내 관심종목 폴더 목록 조회
export const getWatchlists = async (): Promise<Watchlist[]> => {
    const response = await axios.get<ApiResponse<Watchlist[]>>('/api/watchlists');

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '목록 조회 실패');
    }

    return response.data.data;
};

// 내 관심종목 폴더 상세 조회
export const getWatchlistDetail = async (watchlistId: number): Promise<Watchlist> => {
    const response = await axios.get<ApiResponse<Watchlist>>(`/api/watchlists/${watchlistId}`);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '상세 조회 실패');
    }

    return response.data.data;
};

// 새 관심종목 폴더 생성
export const createWatchlist = async (name: string): Promise<number> => {
    const body: WatchlistCreateRequest = { name };
    const response = await axios.post<ApiResponse<number>>('api/watchlists', body);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '폴더 생성 실패');
    }

    return response.data.data;
};

// 관심종목 폴더 이름 변경
export const updateWatchlist = async (watchlistId: number, name: string): Promise<void> => {
    const body: WatchlistUpdateRequest = { name };
    const response = await axios.patch<ApiResponse<void>>(`/api/watchlists/${watchlistId}`, body);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '폴더 이름 변경 실패');
    }
};

// 관심종목 폴더 삭제
export const deleteWatchlist = async (watchlistId: number): Promise<void> => {
    const response = await axios.delete<ApiResponse<void>>(`/api/watchlists/${watchlistId}`);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '폴더 삭제 실패');
    }
};

// 관심종목 폴더 순서 변경
export const sortWatchlists = async (watchlistIds: number[]): Promise<void> => {
    const body: WatchlistSortRequest = { watchlistIds };
    const response = await axios.patch<ApiResponse<void>>('/api/watchlists/sort', body);

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '폴더 순서 변경 실패');
    }
};