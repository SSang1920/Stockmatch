import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
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
        mutationFn: (payload: HoldingPayload) => portfolioApi.addHolding(payload),
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
        mutationFn: ({ holdingId, payload }: { holdingId: number; payload:HoldingPayload }) => portfolioApi.updateHolding(holdingId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] });
        },
        onError: (error: any) => {
            alert(error.response.data.message || '종목 수정 중 오류가 발생했습니다.');
        }
    });
};

export const useDailyHistory = () => {
    return useQuery({
        queryKey: ['portfolio', 'me', 'dailyHistory'],
        queryFn: () => {
            const today = new Date();
            const thirtyDaysAgo = new Date();
            thirtyDaysAgo.setDate(today.getDate() - 30);

            const formatDate = (date: Date) => {
                const year = date.getFullYear();
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const day = String(date.getDate()).padStart(2, '0');
                return `${year}-${month}-${day}`;
            };

            const to = formatDate(today);
            const from = formatDate(thirtyDaysAgo);

            return portfolioApi.getDailyHistory(from, to);
        },
    });
};

export const useTransactionsInfinite = (portfolioId?: number) => {
    return useInfiniteQuery({
        queryKey: ['portfolio', portfolioId, 'transactions', 'infinite'],
        queryFn: ({ pageParam }) => portfolioApi.getTransactions(portfolioId!, pageParam as number, 30),
        initialPageParam: 0,
        getNextPageParam: (lastPage: any, allPages, lastPageParam) => {
            return lastPage.last ? undefined : (lastPageParam as number) + 1;
        },
        enabled: !!portfolioId,
    });
};

export const useAddTransaction = (portfolioId?: number) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ type, payload }: { type: 'BUY' | 'SELL', payload: any }) => {
            if (type === 'BUY') {
                return await portfolioApi.buyTransaction(portfolioId!, payload);
            } else {
                return await portfolioApi.sellTransaction(portfolioId!, payload);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId, 'transactions', 'infinite'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] });
        }
    });
};

export const useDeleteTransaction = (portfolioId?: number) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (transactionId: number) => portfolioApi.deleteTransaction(portfolioId!, transactionId),
        onSuccess: () => {
            alert('거래 기록이 삭제되었습니다.');
            queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId, 'transactions', 'infinite'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] });
        },
        onError: (error: any) => {
            alert(error.response.data.message || '삭제에 실패했습니다.');
        }
    });
};