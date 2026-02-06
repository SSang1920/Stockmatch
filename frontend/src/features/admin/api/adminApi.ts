import axios from '@/lib/axios';
import { DashboardStats } from '../types';
import { ApiResponse } from '@/types/common';

export const fetchDashboardStats = async (): Promise<DashboardStats> => {
    const response = await axios.get<ApiResponse<DashboardStats>>('/admin/dashboard');

    return response.data;
};
