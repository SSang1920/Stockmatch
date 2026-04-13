import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useDailyHistory, usePortfolioValuation } from "../hooks/usePortfolio";
import { Loader2 } from "lucide-react";
import { CartesianGrid, Label, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

interface PortfolioLineChartProps {
    className?: string;
}

export function PortfolioLineChart({ className }: PortfolioLineChartProps) {
    const { data: apiHistoryData, isLoading: historyLoading, isError: historyError } = useDailyHistory();
    const { data: valuationData, isLoading: valuationLoading } = usePortfolioValuation();

    const historyData = apiHistoryData;
    const totalInvested = valuationData?.totalInvested;

    if (historyLoading || valuationLoading) {
        return (
            <Card className="shadow-sm border-none bg-white w-full h-[350px] flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
            </Card>
        );
    }

    if (historyError || !historyData || historyData.length === 0) {
        return (
            <Card className="shadow-sm border-none bg-white w-full h-[350px] flex items-center justify-center text-gray-400">
                최근 자산 변동 내역이 없습니다.
            </Card>
        );
    }

    // 커스텀 툴팁
    const CustomTooltip = ({ active, payload, label }: any) => {
        if (active && payload && payload.length) {
            const dataItem = payload[0].payload;
            const totalValue = Math.floor(dataItem.totalValue || 0);
            const totalInvested = Math.floor(dataItem.totalInvested || 0);
            const totalRate = (dataItem.totalRate || 0) * 100;

            return (
                <div className="bg-white border border-gray-200 p-3 rounded-lg shadow-md z-50">
                    <p className="font-bold text-gray-600 mb-2">{label}</p>
                    <div className="space-y-1">
                        <p className="text-blue-600 font-semibold">
                            평가금액: {totalValue.toLocaleString()}원
                        </p>
                        <p className="text-emerald-500 text-sm font-medium">
                            매수금액: {totalInvested.toLocaleString()}원
                        </p>
                        <p className={`text-sm font-bold ${totalRate >= 0 ? 'text-red-500' : 'text-blue-500'}`}>
                            수익률: {totalRate >= 0 ? "+" : ""}{totalRate.toFixed(2)}%
                        </p>
                    </div>
                </div>
            );
        }
        return null;
    };

    return (
        <Card className={`shadow-sm border-none bg-white w-full flex flex-col h-full ${className}`}>
            <CardHeader className="border-b pb-4">
                <CardTitle className="text-lg">자산 추이 (최근 30일)</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 h-[350px]">
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={historyData} margin={{ top: 10, right: 30, left: 20, bottom: 0 }}>
                        {/* 배경에 연한 점선 그리드 */}
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />

                        {/* X축: 날짜 */}
                        <XAxis
                            dataKey="date"
                            tickFormatter={(date) => date.substring(5)}
                            tick={{ fontSize: 12, fill: '#6b7280' }}
                            tickMargin={10}
                            axisLine={false}
                            tickLine={false}
                        />

                        {/* Y축: 금액 */}
                        <YAxis
                            tickFormatter={(value) => `${Math.floor(value / 10000).toLocaleString()}만`}
                            tick={{ fontSize: 12, fill: '#6b7280' }}
                            axisLine={false}
                            tickLine={false}
                            width={80}
                            domain={[
                                (dataMin) => Math.max(0, Math.floor(dataMin - 100000)),
                                (dataMax) => Math.floor(dataMax + 100000)
                            ]}
                        />

                        <Tooltip content={<CustomTooltip />} />

                        {/* 매수금액 히스토리 선 */}
                        <Line
                            name="매수금액"
                            type="stepAfter" 
                            dataKey="totalInvested"
                            stroke="#10b981"
                            strokeWidth={2}
                            strokeDasharray="5 5"
                            dot={false}
                        />

                        <Line
                            name="평가금액"
                            type="monotone"
                            dataKey="totalValue"
                            stroke="#3b82f6"
                            strokeWidth={3}
                            dot={false}
                            activeDot={{ r: 6, stroke: 'white', strokeWidth: 2 }}
                        />
                    </LineChart>
                </ResponsiveContainer>
            </CardContent>
        </Card>
    );
}