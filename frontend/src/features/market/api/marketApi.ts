import axios from 'axios';
import { MarketOverviewResponse } from '../types/market';

export const fetchMarketOverview = async (): Promise<MarketOverviewResponse> => {
    const response = await axios.get<MarketOverviewResponse>('/api/market/overview');
    return response.data;
};