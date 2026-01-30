import axios from '@/lib/axios';
import {ApiResponse, AiResponseDto} from '../types';

export const fetchAiAnalysis = async (ticker : string) => {
    const response = await axios.get<ApiResponse<AiResponseDto>>(`/analysis/${ticker}/ai`);
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