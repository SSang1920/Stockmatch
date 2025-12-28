import axios from 'axios';
import { MarketOverviewResponse } from '../types/market';

interface ApiResponse<T> {
    success: boolean;
    data: T;
}

export const fetchMarketOverview = async (): Promise<MarketOverviewResponse> => {
    const response = await axios.get<ApiResponse<MarketOverviewResponse>>('/api/market/overview');
    return response.data.data;
};