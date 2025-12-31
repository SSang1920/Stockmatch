import axios from 'axios';
import { MarketOverviewResponse } from '../types/market';
import { StockSearchResponse, MarketTrendResponse } from '../types/stock';

interface ApiResponse<T> {
    success: boolean;
    data: T;
}

// 마켓 오버뷰 조회 API
export const fetchMarketOverview = async (): Promise<MarketOverviewResponse> => {
    const response = await axios.get<ApiResponse<MarketOverviewResponse>>('/api/market/overview');
    return response.data.data;
};

// 주식 검색 API
export const searchStocks = async (query: string): Promise<StockSearchResponse[]> => {
    // 검색어 없으면 빈 배열 리턴
    if (!query) return [];

    const response = await axios.get<ApiResponse<StockSearchResponse[]>>(`/api/stocks/search`, {
        params: { q: query }
    });

    return response.data.data;
}

// 시장 트렌드 조회 API
export const fetchMarketTrends = async (): Promise<MarketTrendResponse> => {
    const response = await axios.get('/api/market/trends');
    return response.data.data;
}