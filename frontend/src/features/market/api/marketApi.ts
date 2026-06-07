import axios, { publicInstance } from '@/lib/axios';
import { MarketOverviewResponse, MarketTrendResponse } from '../types';
import { ApiResponse } from '@/types/common';

// 마켓 오버뷰 조회 API
export const fetchMarketOverview = async (): Promise<MarketOverviewResponse> => {
    const response = await publicInstance.get<ApiResponse<MarketOverviewResponse>>('/market/overview');

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '마켓 오버뷰 조회 실패');
    }

    return response.data.data;
};

// 시장 트렌드 조회 API
export const fetchMarketTrends = async (): Promise<MarketTrendResponse> => {
    const response = await publicInstance.get<ApiResponse<MarketTrendResponse>>('/market/trends');

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '시장 트렌드 조회 실패');
    }

    return response.data.data;
};

// 최신 환율 조회 API
export const getExchangeRate = async () : Promise<number> => {
    const response = await publicInstance.get<ApiResponse<number>>('/exchange-rate/usd-krw');

    if (!response.data.success) {
        throw new Error(response.data.error?.message || '환율 조회 실패');
    }

    return response.data.data;
};