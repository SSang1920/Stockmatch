import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowDownRight, ArrowUpRight, Loader2, Plus } from "lucide-react";
import { useState } from "react";
import { usePortfolioValuation, useTransactionsInfinite } from "./hooks/usePortfolio";
import { Button } from "@/components/ui/button";
import { StockSearchBar } from "../market/components/StockSearchBar";
import { UnifiedTradeModal } from "./components/UnifiedTradeModal";

export default function TransactionHistoryPage() {
    const [searchTerm, setSearchTerm] = useState("");
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const { data: valuationData, isLoading: isValuationLoading } = usePortfolioValuation();
    const portfolioId = valuationData?.portfolioId;

    const {
        data,
        isLoading: isTxLoading,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage
    } = useTransactionsInfinite(portfolioId);

    // 로딩 처리
    if (isValuationLoading || isTxLoading) {
        return <div className="p-10 flex justify-center"><Loader2 className="h-8 w-8 animate-spin text-gray-400" /></div>;
    }

    const allTransactions = data?.pages.flatMap(page => page.content) || [];

    // 검색 필터
    const filteredTransactions = allTransactions.filter((t: any) => {
        const name = t.name || "";
        const ticker = t.ticker || "";
        return name.includes(searchTerm) || ticker.toLowerCase().includes(searchTerm.toLowerCase());
    });

    return (
        <div className="p-6 space-y-6 max-w-5xl mx-auto">
            {/* 헤더 영역 */}
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">거래 내역</h1>
                    <p className="text-sm text-gray-500 mt-1">
                        지금까지의 매수/매도 기록을 확인하세요.
                    </p>
                </div>

                <div className="flex items-center gap-3 w-full md:w-auto z-10">
                    {/* 거래 추가 버튼 */}
                    <Button
                        onClick={() => setIsAddModalOpen(true)}
                        className="bg-gray-800 hover:bg-gray-900 text-white shrink-0"
                    >
                        <Plus className="w-4 h-4 mr-1" /> 새 거래
                    </Button>

                    {/* 검색창 */}
                    <div className="w-full md:w-64">
                        <StockSearchBar
                            onSelectStock={(stock) => setSearchTerm(stock.ticker)}
                            hideRecent={true}
                        />
                    </div>
                </div>

            </div>

            {/* 거래 내역 리스트 카드 */}
            <Card className="shadow-sm border-gray-200">
                <CardHeader className="border-b pb-4 px-6 pt-6">
                    <CardTitle className="text-lg font-bold text-gray-700">최근 거래</CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                    {filteredTransactions.length === 0 ? (
                        <div className="p-8 text-center text-gray-500">
                            검색된 거래 내역이 없습니다.
                        </div>
                    ) : (
                        <div className="divide-y divide-gray-100">
                            {filteredTransactions.map((tx: any) => {
                                const isInitialBuy = tx.type === 'INITIAL_BUY';
                                const isBuy = tx.type === 'BUY';
                                const isSell = tx.type === 'SELL';

                                let typeLabel = '';
                                if (isInitialBuy) typeLabel = '초기보유';
                                else if (isBuy) typeLabel = '매수';
                                else if (isSell) typeLabel = '매도';

                                const isPositiveAction = isBuy || isInitialBuy;
                                const formattedDateTime = tx.tradeAt ? tx.tradeAt.replace('T', ' ').substring(0, 16) : '날짜 없음';

                                const isKoreanStock = /^\d+$/.test(tx.ticker || "");
                                const currency = tx.currency || (isKoreanStock ? 'KRW' : 'USD');
                                const currencySymbol = currency === 'USD' ? '$' : '₩';

                                const totalAmount = tx.totalAmount ? Number(tx.totalAmount).toLocaleString() : '--';
                                const price = tx.price ? Number(tx.price).toLocaleString() : '--';

                                return (
                                    <div key={tx.id} className="p-4 flex items-center justify-between hover:bg-gray-50 transition-colors">
                                        {/* 왼쪽: 아이콘 & 종목 정보 */}
                                        <div className="flex items-center gap-4">
                                            <div className={`p-2 rounded-full ${isBuy ? 'bg-red-100 text-red-600' : 'bg-blue-100 text-blue-600'}`}>
                                                {isBuy ? <ArrowUpRight className="h-5 w-5" /> : <ArrowDownRight className="h-5 w-5" />}
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-semibold text-gray-800">{tx.name || '--'}</span>
                                                    <span className="text-xs text-gray-500">{tx.ticker}</span>
                                                </div>
                                                <div className="text-xs text-gray-400 mt-0.5">
                                                    {formattedDateTime}
                                                </div>
                                                {tx.memo && (
                                                    <div className="mt-1.5 inline-block bg-gray-100 text-gray-600 text-xs px-2 py-0.5 rounded-md">
                                                        {tx.memo}
                                                    </div>
                                                )}
                                            </div>
                                        </div>

                                        {/* 오른쪽: 매수/매도 정보 & 금액 */}
                                        <div className="text-right">
                                            <div className="flex items-center justify-end gap-2 mb-0.5">
                                                <Badge variant={isPositiveAction ? 'destructive' : 'default'} className={isPositiveAction ? 'bg-red-500 hover:bg-red-600' : 'bg-blue-500 hover:bg-blue-600'}>
                                                    {typeLabel}
                                                </Badge>
                                                <span className="font-bold text-gray-800">
                                                    {currencySymbol}{totalAmount} <span className="text-xs text-gray-500 font-normal">{currency}</span>
                                                </span>
                                            </div>
                                            <div className="text-sm text-gray-500">
                                                {currencySymbol}{price} × {tx.quantity}주
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </CardContent>
            </Card>

            {hasNextPage && (
                <div className="flex justify-center pt-4 pb-8">
                    <Button
                        variant="outline"
                        className="w-full md:w-1/3 text-gray-600 bg-white hover:bg-gray-50"
                        onClick={() => fetchNextPage()}
                        disabled={isFetchingNextPage}
                    >
                        {isFetchingNextPage ? (
                            <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> 불러오는 중...</>
                        ) : (
                            '더 보기'
                        )}
                    </Button>
                </div>
            )}

            {/* 모달 컴포넌트 마운트 */}
            {portfolioId && (
                <UnifiedTradeModal
                    portfolioId={portfolioId}
                    isOpen={isAddModalOpen}
                    onClose={() => setIsAddModalOpen(false)}
                />
            )}
        </div>
    );
}