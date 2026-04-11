import axios from '@/lib/axios';
import {ApiResponse, AiResponseDto, AnalysisHistoryListResponse} from '../types';

export const fetchAiStockAnalysis = async (ticker : string) => {
    const response = await axios.get<ApiResponse<AiResponseDto>>(`/analysis/${ticker}/stock-ai`);
    return response.data;
};

export const fetchAiFinancialAnalysis = async (ticker : string) => {
    const response = await axios.get<ApiResponse<AiResponseDto>>(`/analysis/${ticker}/financial-ai`);
    return response.data;
};

export const fetchAiPortfolioAnalysis = async (comment : string) => {
    const response = await axios.post<ApiResponse<AiResponseDto>>(`/analysis/portfolio-ai`, {comment});
    return response.data;
};

export const fetchUserHistoryList = async () => {
    const response = await axios.get<ApiResponse<AnalysisHistoryListResponse[]>>('/analysis/history');
    return response.data;
};

export const fetchHistoryDetail = async (id: number) => {
    const response = await axios.get<ApiResponse<AiResponseDto>>(`/analysis/history/${id}`);
    return response.data;
};