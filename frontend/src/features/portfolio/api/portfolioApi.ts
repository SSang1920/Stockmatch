import axios from "@/lib/axios"
import { ApiResponse } from "@/types/common"
import { HoldingItem, HoldingPayload, PortfolioDailySummaryResponse, PortfolioProfitStatsResponse, PortfolioValuationResponse } from "../types";

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

    // 종목 추가
    addHolding: async (payload: HoldingPayload) => {
        const response = await axios.post<ApiResponse<any>>('/portfolio/me/holdings', payload);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '종목 추가에 실패했습니다.');
        }
        
        return response.data.data;
    },

    // 종목 수정
    updateHolding: async (holdingId: number, payload: HoldingPayload) => {
        const response = await axios.put<ApiResponse<any>>(`/portfolio/me/holdings/${holdingId}`, payload);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '종목 수정에 실패');
        }

        return response.data.data;
    },

    // 거래 내역 조회
    getTransactions: async (portfolioId: number, page: number = 0, size: number = 30) => {
        const response = await axios.get<ApiResponse<any[]>>(`/portfolio/${portfolioId}/transaction?page=${page}&size=${size}`);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '거래 내역 조회에 실패');
        }

        return response.data.data;
    },

    // 매수
    buyTransaction: async (portfolioId: number, payload: any) => {
        const response = await axios.post<ApiResponse<any>>(`/portfolio/${portfolioId}/transaction/buy`, payload);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || ' 매수 실패');
        }

        return response.data.data;
    },

    // 매도
    sellTransaction: async (portfolioId: number, payload: any) => {
        const response = await axios.post<ApiResponse<any>>(`/portfolio/${portfolioId}/transaction/sell`, payload);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || ' 매도 실패');
        }

        return response.data.data;
    },

    // 거래 내역 삭제
    deleteTransaction: async (portfolioId: number, transactionId: number) => {
        const response = await axios.delete<ApiResponse<any>>(`/portfolio/${portfolioId}/transaction/${transactionId}`);

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '거래 내역 삭제 실패');
        }

        return response.data.data;
    },

    // 포트폴리오 수익 통계 조회 (누적, 연간, 월간, 일간)
    getStats: async (portfolioId: number, year: string, month: string) => {
        const response = await axios.get<ApiResponse<PortfolioProfitStatsResponse>>(
            `/portfolio/${portfolioId}/stats`,
            {
                params: { year, month }
            }
        );

        if (!response.data.success) {
            throw new Error(response.data.error?.message || '포트폴리오 수익 통계 조회 실패');
        }

        return response.data.data;
    }
}