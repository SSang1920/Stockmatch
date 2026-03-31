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
    const [quantity, setQuantity] = useState<string>("");
    const [avgPrice, setAvgPrice] = useState<string>("");

    // 모달이 열릴 때 수정 모드면 기존 데이터 세팅
    useEffect(() => {
        if (holding) {
            setTicker(holding.ticker);
            setSelectedName(holding.krName || holding.name || "");
            setSelectedMarket(holding.currency === 'USD' ? 'US' : 'KR');
            setQuantity(String(holding.quantity));
            setAvgPrice(String(holding.avgPrice));
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
            if (!holding.holdingId) {
                alert("수정할 종목 고유 ID가 없습니다.");
                return;
            }
            updateHolding({ holdingId: holding.holdingId, payload }, { onSuccess: onClose });
        } else {
            addHolding(payload, { onSuccess: onClose });
        }
    };

    const isPending = isAdding || isUpdating;
    const isUsMarket = ["NASDAQ", "NYSE", "AMEX", "US"].includes(selectedMarket.toUpperCase());

    // 숫자에 콤마 찍어주는 함수
    const addComma = (value: string | number) => {
        if (!value) return "";
        const parts = value.toString().split(".");
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        return parts.join(".");
    };

    // 콤마를 다시 빼서 순수 숫자로 돌려주는 함수
    const removeComma = (value: string) => {
        return value.replace(/,/g, "");
    };

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
                                type="text"
                                value={addComma(quantity)}
                                onChange={(e) => {
                                    const rawValue = removeComma(e.target.value);
                                    if (/^\d*\.?\d*$/.test(rawValue)) {
                                        setQuantity(rawValue);
                                    }
                                }}
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
                            <Input
                                id="avgPrice"
                                type="text"
                                value={addComma(avgPrice)}
                                onChange={(e) => {
                                    const rawValue = removeComma(e.target.value);
                                    if (/^\d*\.?\d*$/.test(rawValue)) {
                                        setAvgPrice(rawValue);
                                    }
                                }}
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