import axios from '@/lib/axios';
import { MarketOverviewResponse, StockSearchResponse, MarketTrendResponse } from '../types';
import { ApiResponse } from '@/types/common';

// 마켓 오버뷰 조회 API
export const fetchMarketOverview = async (): Promise<MarketOverviewResponse> => {
    const response = await axios.get<ApiResponse<MarketOverviewResponse>>('/market/overview');

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '마켓 오버뷰 조회 실패');
    }

    return response.data.data;
};

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

// 시장 트렌드 조회 API
export const fetchMarketTrends = async (): Promise<MarketTrendResponse> => {
    const response = await axios.get<ApiResponse<MarketTrendResponse>>('/market/trends');

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '시장 트렌드 조회 실패');
    }

    return response.data.data;
};