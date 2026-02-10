import axios from '@/lib/axios';
import { ApiResponse } from "@/types/common";
import { StockChartItem, StockDetailResponse, StockSearchResponse } from "../types";

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

// 차트 데이터 조회 API
export const getStockChart = async (ticker: string): Promise<StockChartItem[]> => {
    const today = new Date();
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(today.getFullYear() - 1);

    const response = await axios.get<ApiResponse<any[]>>(`/stocks/daily-price/${ticker}`, {
        params: {
            from: formatDate(oneYearAgo),
            to: formatDate(today)
        }
    });

    if (!response.data.success || !response.data.data) {
        return [];
    }

    const rawData = response.data.data;

    return rawData.map((item: any) => ({
        date: item.date,
        open: item.openPrice ?? item.open_price ?? item.open ?? 0,
        high: item.highPrice ?? item.high_price ?? item.high ?? 0,
        low: item.lowPrice ?? item.low_price ?? item.low?? 0,
        close: item.closePrice ?? item.close_price ?? item.close ?? 0
    }))
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
}

// 날짜 변환 유틸 함수
const formatDate = (date: Date) => {
    return date.toISOString().split('T')[0];
}