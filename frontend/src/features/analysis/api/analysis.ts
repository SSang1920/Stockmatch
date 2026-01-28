import axios from '@/lib/axios';
import {ApiResponse, AiResponseDto} from '../types';

export const fetchAiAnalysis = async (ticker : string) => {
    const response = await axios.get<ApiResponse<AiResponseDto>>(`/analysis/${ticker}/ai`);
    return response.data;
    };