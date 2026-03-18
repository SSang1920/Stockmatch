import React, { useEffect, useState } from "react";
import { HoldingItem } from "../types";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAddHolding, useUpdateHolding } from "../hooks/usePortfolio";
import { StockSearchBar } from "@/features/market/components/StockSearchBar";
import { CheckCircle2 } from "lucide-react";

interface HoldingFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    holding?: HoldingItem | null;
}

export function HoldingFormModal({ isOpen, onClose, holding }: HoldingFormModalProps) {
    const { mutate: addHolding, isPending: isAdding } = useAddHolding();
    const { mutate: updateHolding, isPending: isUpdating } = useUpdateHolding();

    const [ticker, setTicker] = useState("");
    const [selectedName, setSelectedName] = useState("");
    const [selectedMarket, setSelectedMarket] = useState("");
    const [quantity, setQuantity] = useState<number | "">("");
    const [avgPrice, setAvgPrice] = useState<number | "">("");

    // 모달이 열릴 때 수정 모드면 기존 데이터 세팅
    useEffect(() => {
        if (holding) {
            setTicker(holding.ticker);
            setSelectedName(holding.krName || holding.name || "");
            setSelectedMarket(holding.currency === 'USD' ? 'US' : 'KR');
            setQuantity(holding.quantity);
            setAvgPrice(holding.avgPrice);
        } else {
            setTicker("");
            setSelectedName("");
            setSelectedMarket("");
            setQuantity("");
            setAvgPrice("");
        }
    }, [holding, isOpen]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!ticker || !quantity || !avgPrice) return alert("모든 항목을 입력해주세요.");

        const payload = {
            ticker: ticker.toUpperCase(),
            quantity: Number(quantity),
            avgPrice: Number(avgPrice),
        };

        if (holding) {
            updateHolding(payload, { onSuccess: onClose });
        } else {
            addHolding(payload, { onSuccess: onClose });
        }
    };

    const isPending = isAdding || isUpdating;
    const isUsMarket = ["NASDAQ", "NYSE", "AMEX", "US"].includes(selectedMarket.toUpperCase());

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[425px] bg-white overflow-visible">
                <DialogHeader>
                    <DialogTitle>{holding ? "보유 종목 수정" : "새 종목 추가"}</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4 py-4">

                    <div className="space-y-2">
                        <Label>종목 검색</Label>
                        {holding ? (
                            // 수정 모드: 티커 변경 불가
                            <Input value={`${selectedName} (${ticker})`} disabled className="bg-gray-50 text-gray-500" />
                        ) : (
                            // 추가 모드: 검색바 표시
                            <div className="flex flex-col gap-2">
                                <StockSearchBar
                                    onSelectStock={(stock) => {
                                        setTicker(stock.ticker);
                                        setSelectedName(stock.name);
                                        setSelectedMarket(stock.market);
                                    }}
                                />
                                {/* 선택 완료 피드백 UI */}
                                {ticker && (
                                    <div className="flex items-center text-sm text-green-600 bg-green-50 p-2 rounded-md">
                                        <CheckCircle2 className="w-4 h-4 mr-1" />
                                        선택됨: <span className="font-bold ml-1">{selectedName} ({ticker})</span>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="quantity">보유 수량</Label>
                            <Input
                                id="quantity"
                                type="number"
                                step="any"
                                value={quantity}
                                onChange={(e) => setQuantity(e.target.value ? Number(e.target.value) : "")}
                                placeholder="예: 10"
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="avgPrice">
                                매입 평단가
                                {selectedMarket && (
                                    <span className="text-blue-500 font-bold ml-1">
                                        ({isUsMarket ? "달러 $" : "원 ₩"})
                                    </span>
                                )}
                            </Label>
                            <div className="relative">
                                {selectedMarket && (
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <span className="text-gray-500 sm:text-sm">
                                            {isUsMarket ? "$" : "₩"}
                                        </span>
                                    </div>
                                )}
                            </div>
                            <Input
                                id="avgPrice"
                                type="number"
                                step="any"
                                value={avgPrice}
                                onChange={(e) => setAvgPrice(e.target.value ? Number(e.target.value) : "")}
                                placeholder={isUsMarket ? "예: 150.5" : "예: 75000"}
                                className={selectedMarket ? "pl-7" : ""}
                            />
                        </div>
                    </div>

                    <DialogFooter className="mt-6">
                        <Button type="button" variant="outline" onClick={onClose}>취소</Button>
                        <Button type="submit" disabled={isPending || !ticker}>
                            {isPending ? "처리중..." : "저장하기"}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}