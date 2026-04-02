import { useEffect, useState } from "react";
import { useAddHolding, useAddTransaction, useUpdateHolding } from "../hooks/usePortfolio";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { StockSearchBar } from "@/features/market/components/StockSearchBar";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";

interface UnifiedTradeModalProps {
    isOpen: boolean;
    onClose: () => void;
    portfolioId: number;
    holdingToEdit?: any | null;
}

type TradeMode = 'INITIAL' | 'BUY' | 'SELL';

export function UnifiedTradeModal({ isOpen, onClose, portfolioId, holdingToEdit }: UnifiedTradeModalProps) {
    // API 훅
    const { mutate: addHolding, isPending: isAddingHolding } = useAddHolding();
    const { mutate: updateHolding, isPending: isUpdatingHolding } = useUpdateHolding();
    const addTxMutation = useAddTransaction(portfolioId);

    // 상태 관리
    const [mode, setMode] = useState<TradeMode>('INITIAL');
    const [selectedStock, setSelectedStock] = useState<any>(null);
    const [quantity, setQuantity] = useState<string>("");
    const [price, setPrice] = useState<string>("");
    const [memo, setMemo] = useState<string>("");

    // 모달이 열릴 때 데이터 초기화 (수정 모드일 때는 세팅)
    useEffect(() => {
        if (holdingToEdit) {
            setMode('INITIAL'); // 수정은 초기보유만 가능하도록 고정
            setSelectedStock({
                ticker: holdingToEdit.ticker,
                name: holdingToEdit.krName || holdingToEdit.name || "",
                market: holdingToEdit.currency === 'USD' ? 'US' : 'KR'
            });
            setQuantity(String(holdingToEdit.quantity));
            setPrice(String(holdingToEdit.avgPrice));
            setMemo("");
        } else {
            // 새 창 열릴 때 싹 비우기
            setMode('BUY');
            setSelectedStock(null);
            setQuantity("");
            setPrice("");
            setMemo("");
        }
    }, [holdingToEdit, isOpen]);

    // 숫자 콤마 유틸리티
    const addComma = (value: string | number) => {
        if (!value) return "";
        const parts = value.toString().split(".");
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        return parts.join(".");
    };
    const removeComma = (value: string) => value.replace(/,/g, "");

    // 통화 기호 결정
    const currencySymbol = (() => {
        if (!selectedStock?.market) return "";
        const upperMarket = selectedStock.market.toUpperCase();
        const usMarkets = ["NASDAQ", "NYSE", "AMEX", "US", "NAS", "NYS"];
        return usMarkets.includes(upperMarket) ? "$" : "₩";
    })();

    // 폼 제출 로직
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedStock) return alert("종목을 선택해주세요.");
        if (!quantity || !price) return alert("수량과 단가를 입력해주세요.");

        const numQuantity = Number(removeComma(quantity));
        const numPrice = Number(removeComma(price));

        if (mode === 'INITIAL') {
            // 기존 주식 등록
            const payload = { ticker: selectedStock.ticker.toUpperCase(), quantity: numQuantity, avgPrice: numPrice };
            if (holdingToEdit) {
                updateHolding({ holdingId: holdingToEdit.holdingId, payload }, { onSuccess: onClose });
            } else {
                addHolding(payload, { onSuccess: onClose });
            }
        } else {
            // 새 거래
            if (!selectedStock.id) return alert("이 종목은 거래 내역에 추가할 수 없습니다.");

            const payload = {
                securityId: selectedStock.id,
                price: numPrice,
                quantity: numQuantity,
                fee: 0,
                tradeAt: new Date().toISOString(),
                memo: memo
            };
            addTxMutation.mutate({ type: mode, payload }, { onSuccess: onClose });
        }
    };

    const isPending = isAddingHolding || isUpdatingHolding || addTxMutation.isPending;

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[425px] bg-white overflow-visible">
                <DialogHeader>
                    <DialogTitle>{holdingToEdit ? "보유 종목 수정" : "기록 추가"}</DialogTitle>
                </DialogHeader>

                <form onSubmit={handleSubmit} className="space-y-5 py-2">
                    {!holdingToEdit && (
                        <div className="flex bg-gray-100 rounded-lg p-1">
                            <button
                                type="button"
                                onClick={() => setMode('INITIAL')}
                                className={`flex-1 py-2 text-xs font-bold rounded-md transition-all ${mode === 'INITIAL' ? 'bg-gray-800 text-white shadow' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                기존 보유 등록
                            </button>
                            <button
                                type="button"
                                onClick={() => setMode('BUY')}
                                className={`flex-1 py-2 text-xs font-bold rounded-md transition-all ${mode === 'BUY' ? 'bg-red-500 text-white shadow' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                새 매수 (BUY)
                            </button>
                            <button
                                type="button"
                                onClick={() => setMode('SELL')}
                                className={`flex-1 py-2 text-xs font-bold rounded-md transition-all ${mode === 'SELL' ? 'bg-blue-500 text-white shadow' : 'text-gray-500 hover:text-gray-700'}`}
                            >
                                새 매도 (SELL)
                            </button>
                        </div>
                    )}

                    {/* 종목 선택 */}
                    <div className="space-y-2">
                        <Label>종목 선택</Label>
                        {holdingToEdit ? (
                            <Input value={`${selectedStock?.name} (${selectedStock?.ticker})`} disabled className="bg-gray-50 text-gray-500" />
                        ) : selectedStock ? (
                            <div className="flex items-center justify-between p-3 border rounded-lg bg-gray-50">
                                <span className="font-semibold">
                                    {selectedStock.name} <span className="text-sm font-normal text-gray-500">({selectedStock.ticker})</span>
                                </span>
                                <button type="button" onClick={() => setSelectedStock(null)} className="text-xs text-gray-500 underline hover:text-gray-800">
                                    다시 검색
                                </button>
                            </div>
                        ) : (
                            <StockSearchBar onSelectStock={(stock) => setSelectedStock(stock)} />
                        )}
                    </div>

                    {/* 단가 & 수량 */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="price">{mode === 'INITIAL' ? '매입 평단가' : '체결 단가'}</Label>
                            <div className="relative flex items-center">
                                {currencySymbol && <span className="absolute left-3 text-gray-500 font-bold pointer-events-none z-10">{currencySymbol}</span>}
                                <Input
                                    id="price" type="text"
                                    placeholder={currencySymbol === "$" ? "150.50" : "75,000"}
                                    value={addComma(price)}
                                    onChange={(e) => {
                                        const val = removeComma(e.target.value);
                                        if (/^\d*\.?\d*$/.test(val)) setPrice(val);
                                    }}
                                    className={`bg-white ${currencySymbol ? 'pl-8' : ''}`}
                                />
                            </div>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="quantity">수량 (주)</Label>
                            <Input
                                id="quantity" type="text" placeholder="예: 10"
                                value={addComma(quantity)}
                                onChange={(e) => {
                                    const val = removeComma(e.target.value);
                                    if (/^\d*\.?\d*$/.test(val)) setQuantity(val);
                                }}
                            />
                        </div>
                    </div>

                    {/* 메모 (초기보유가 아닐 때만 표시) */}
                    {mode !== 'INITIAL' && (
                        <div className="space-y-2">
                            <Label htmlFor="memo">메모 (선택)</Label>
                            <Input id="memo" placeholder="예: 물타기, 수익 실현 등" value={memo} onChange={(e) => setMemo(e.target.value)} />
                        </div>
                    )}

                    {/* 제출 버튼 */}
                    <DialogFooter className="mt-6 pt-2">
                        <Button type="button" variant="outline" onClick={onClose}>취소</Button>
                        <Button
                            type="submit"
                            disabled={isPending || !selectedStock}
                            className={
                                mode === 'BUY' ? 'bg-red-500 hover:bg-red-600 text-white' :
                                    mode === 'SELL' ? 'bg-blue-500 hover:bg-blue-600 text-white' :
                                        'bg-gray-800 hover:bg-gray-900 text-white'
                            }
                        >
                            {isPending ? <Loader2 className="w-5 h-5 animate-spin mx-auto" /> :
                                mode === 'BUY' ? '매수 기록하기' :
                                    mode === 'SELL' ? '매도 기록하기' :
                                        '포트폴리오에 등록'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}