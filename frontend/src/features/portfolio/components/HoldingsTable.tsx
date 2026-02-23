import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { HoldingItem } from "../types/portfolio";
import { useDeleteHolding } from "../hooks/usePortfolio";
import { Button } from "@/components/ui/button";
import { Loader2, Trash2 } from "lucide-react";

interface HoldingsTableProps {
    holdings: HoldingItem[];
    exchangeRate: number;
}

export function HoldingsTable({ holdings, exchangeRate }: HoldingsTableProps) {
    const { mutate: deleteHolding, isPending } = useDeleteHolding();

    const handleDelete = (id: number, ticker: string) => {
        if (confirm(`${ticker} 종목을 삭제하시겠습니까?`)) {
            deleteHolding(id);
        }
    };

    return (
        <Card className="shadow-sm border-none bg-white h-full">
            <CardHeader className="border-b pb-4">
                <div className="flex items-center justify-between">
                    <CardTitle className="text-lg">보유 종목</CardTitle>
                    <span className="text-xs text-gray-400">기준 환율: {exchangeRate.toLocaleString()}원/$</span>
                </div>
            </CardHeader>
            <CardContent className="p-0">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 text-gray-500">
                            <tr>
                                <th className="px-4 py-3 font-medium">종목명</th>
                                <th className="px-4 py-3 font-medium text-right">보유수량</th>
                                <th className="px-4 py-3 font-medium text-right">평가금액</th>
                                <th className="px-4 py-3 font-medium text-right">평가손익</th>
                                <th className="px-4 py-3 font-medium text-right">수익률</th>
                                <th className="px-4 py-3 font-medium text-right">관리</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y">
                            {holdings.map((item) => {
                                const isPlus = item.profit >= 0;
                                const colorClass = isPlus ? "text-red-500" : "text-blue-500";

                                return (
                                    <tr key={item.ticker} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-4 py-3">
                                            <div className="font-bold text-gray-800">
                                                {item.krName || item.name}
                                            </div>
                                            <div className="text-xs text-gray-500 truncate max-w-[120px]">
                                                {item.ticker}
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            {item.quantity.toLocaleString()}주
                                        </td>
                                        <td className="px-4 py-3 text-right font-medium">
                                            <div>{Math.floor(item.valuation).toLocaleString()}원</div>
                                            {item.currency === "USD" && (
                                                <div className="text-xs text-gray-400">
                                                    ${(item.currentPrice * item.quantity).toFixed(2)}
                                                </div>
                                            )}
                                        </td>
                                        <td className={`px-4 py-3 text-right font-medium ${colorClass}`}>
                                            {isPlus ? "+" : ""}
                                            {Math.floor(item.profit).toLocaleString()}원
                                        </td>
                                        <td className={`px-4 py-3 text-right font-bold ${colorClass}`}>
                                            {item.returnRate >= 0 ? "+" : ""}
                                            {(item.returnRate * 100).toFixed(2)}%
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="h-8 w-8 text-gray-400 hover:text-red-500"
                                                onClick={() => handleDelete(item.holdingId, item.ticker)}
                                                disabled={isPending}
                                            >
                                                {isPending ? (
                                                    <Loader2 className="h-4 w-4 animate-spin" />
                                                ) : (
                                                    <Trash2 className="h-4 w-4" />
                                                )}
                                            </Button>
                                        </td>
                                    </tr>
                                );
                            })}
                            {holdings.length === 0 && (
                                <tr>
                                    <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                                        보유한 주식이 없습니다.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </CardContent>
        </Card>
    )
}