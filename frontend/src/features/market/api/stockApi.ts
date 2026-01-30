import axios from '@/lib/axios';
import { ApiResponse } from "@/types/common";
import { StockDetailResponse, StockSearchResponse } from "../types";

// 주식 검색 API
export const searchStocks = async (query: string): Promise<StockSearchResponse[]> => {
    // 검색어 없으면 빈 배열 리턴
    if (!query) return [];

    const response = await axios.get<ApiResponse<StockSearchResponse[]>>(`/stocks/search`, {
        params: { q: query }
    });

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '주식 검색 실패');
    }

    return response.data.data;
};

// 주식 상세 정보 조회 API
export const getStockDetail = async (market: string, ticker: string): Promise<StockDetailResponse> => {
    let url = '';

    const marketUpper = market.toUpperCase();
    if (['NASDAQ', 'NYSE', 'US'].includes(marketUpper)) {
        url = `/stocks/us/${ticker}`;
    } else {
        url = `/stocks/kr/${ticker}`;
    }

    const response = await axios.get<ApiResponse<StockDetailResponse>>(url);

    if(!response.data.success) {
        throw new Error(response.data.error?.message || '주식 상세 정보 조회 실패');
    }

    return response.data.data;
}