import axios from "@/lib/axios"
import { ApiResponse } from "@/types/common"
import { HoldingItem, PortfolioDailySummaryResponse, PortfolioValuationResponse } from "../types";
import { HoldingPayload } from "../hooks/usePortfolio";

export const portfolioApi = {
    // 내 포트폴리오 요약 및 평가액 조회
    getMyValuation: async () => {
        const response = await axios.get<ApiResponse<PortfolioValuationResponse>>('/portfolio/me/valuation');

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '포트폴리오 요약 및 평가액 조회 실패');
        }

        return response.data.data;
    },

    // 내 보유 종목 리스트 조회
    getMyHoldings: async () => {
        const response = await axios.get<ApiResponse<HoldingItem[]>>('/portfolio/me/holdings');

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '내 보유 종목 리스트 조회 실패');
        }

        return response.data.data;
    },

    // 종목 삭제
    deleteHolding: async (holdingId: number) => {
        const response = await axios.delete<ApiResponse<void>>(`/portfolio/me/holdings/${holdingId}`);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '종목 삭제 실패');
        }

        return response.data.data;
    },

    // 일별 자산 히스토리 조회 (차트용)
    getDailyHistory: async (from: string, to: string) => {
        const response = await axios.get<ApiResponse<PortfolioDailySummaryResponse[]>>(
            '/portfolio/me/valuation/daily',
            { params: { from, to } }
        );

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '일별 자산 히스토리 조회 실패');
        }

        return response.data.data;
    },

    // 종목 추가 및 수정 (매수/업데이트)
    saveHolding: async (payload: HoldingPayload) => {
        const response = await axios.post<ApiResponse<HoldingItem>>('/portfolio/me/holdings', payload);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '종목 저장에 실패했습니다.');
        }

        return response.data.data;
    },
}