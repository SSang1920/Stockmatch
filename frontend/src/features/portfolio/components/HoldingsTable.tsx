import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { HoldingItem } from "../types/portfolio";

interface Props {
    holdings: HoldingItem[];
    rate: number;
}

export function HoldingsTable({ holdings, rate }: Props) {
    return (
        <Card className="shadow-sm border-none bg-white h-full">
            <CardHeader className="border-b pb-4">
                <div className="flex items-center justify-between">
                    <CardTitle className="text-lg">보유 종목</CardTitle>
                    <span className="text-xs text-gray-400">기준 환율: {rate}원/$</span>
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
                            </tr>
                        </thead>
                        <tbody className="divide-y">
                            {holdings.map((item) => {
                                const currentVal = item.currentPrice * item.quantity;
                                const investVal = item.avgPrice * item.quantity;
                                const profit = currentVal - investVal;
                                const profitRate = investVal === 0 ? 0 : (profit / investVal) * 100;

                                // 원화 환산
                                const displayCurrentKrw = item.currency === 'USD' ? currentVal * rate : currentVal;
                                const displayProfitKrw = item.currency === 'USD' ? profit * rate : profit;

                                const isPlus = profit >= 0;
                                const colorClass = isPlus ? "text-red-500" : "text-blue-500";

                                return (
                                    <tr key={item.ticker} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-4 py-3">
                                            <div className="font-bold text-gray-800">{item.krName ? item.krName : item.name}</div>
                                            <div className="text-xs text-gray-500 truncate max-w-[120px]">{item.ticker}</div>
                                        </td>
                                        <td className="px-4 py-3 text-right">{item.quantity}주</td>
                                        <td className="px-4 py-3 text-right font-medium">
                                            {Math.floor(displayCurrentKrw).toLocaleString()}원
                                            {item.currency === 'USD' && <div className="text-xs text-gray-400">${currentVal.toFixed(2)}</div>}
                                        </td>
                                        <td className={`px-4 py-3 text-right font-medium ${colorClass}`}>
                                            {isPlus ? "+" : ""}{Math.floor(displayProfitKrw).toLocaleString()}원
                                        </td>
                                        <td className={`px-4 py-3 text-right font-bold ${colorClass}`}>
                                            {profitRate.toFixed(2)}%
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