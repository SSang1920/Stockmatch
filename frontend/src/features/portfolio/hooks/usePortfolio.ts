import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { portfolioApi } from "../api/portfolioApi"
import { HoldingPayload } from "../types";

export const usePortfolioValuation = () => {
    return useQuery({
        queryKey: ['portfolio', 'me', 'valuation'],
        queryFn: portfolioApi.getMyValuation,
        refetchInterval: 60000,
    });
};

export const useHoldings = () => {
    return useQuery({
        queryKey: ['portfolio', 'me', 'holdings'],
        queryFn: portfolioApi.getMyHoldings,
    });
};

export const useDeleteHolding = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (holidingId: number) => portfolioApi.deleteHolding(holidingId),
        onSuccess: () => {
            // 삭제 성공 시 평가액과 보유종목 리스트를 다시 불러옴
            queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] });
        },
    });
};

export const useAddHolding = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: HoldingPayload) => portfolioApi.saveHolding(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] });
        },
        onError: (error: any) => {
            alert(error.response.data.message || '종목 추가 중 오류가 발생했습니다.');
        }
    });
};

export const useUpdateHolding = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: HoldingPayload) => portfolioApi.saveHolding(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] });
        },
        onError: (error: any) => {
            alert(error.response.data.message || '종목 수정 중 오류가 발생했습니다.');
        }
    });
};