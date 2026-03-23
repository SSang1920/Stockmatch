import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useDailyHistory, usePortfolioValuation } from "../hooks/usePortfolio";
import { Loader2 } from "lucide-react";
import { CartesianGrid, Label, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export function PortfolioLineChart() {
    const { data: apiHistoryData, isLoading: historyLoading, isError: historyError } = useDailyHistory();
    // const { data: valuationData, isLoading: valuationLoading } = usePortfolioValuation();

    const MOCK_HISTORY_DATA = [
        { date: "2026-03-10", totalValue: 9800000, totalPnlRate: -0.02 }, // 손실 구간 시작
        { date: "2026-03-11", totalValue: 9500000, totalPnlRate: -0.05 },
        { date: "2026-03-12", totalValue: 10000000, totalPnlRate: 0.00 }, // 원금 도달
        { date: "2026-03-13", totalValue: 10800000, totalPnlRate: 0.08 }, // 수익 구간 진입
        { date: "2026-03-14", totalValue: 11500000, totalPnlRate: 0.15 },
        { date: "2026-03-15", totalValue: 11200000, totalPnlRate: 0.12 },
        { date: "2026-03-16", totalValue: 11800000, totalPnlRate: 0.18 },
        { date: "2026-03-17", totalValue: 12500000, totalPnlRate: 0.25 },
        { date: "2026-03-18", totalValue: 12100000, totalPnlRate: 0.21 },
        { date: "2026-03-19", totalValue: 13000000, totalPnlRate: 0.30 },
    ];

    const historyData = MOCK_HISTORY_DATA;
    const totalInvested = 10000000; // 가로선을 위한 가짜 총 매수금액 (천만원)

    if (historyLoading) {
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
            return (
                <div className="bg-white border border-gray-200 p-3 rounded-lg shadow-md z-50">
                    <p className="font-bold text-gray-600 mb-1">{label}</p>
                    <p className="text-blue-600 font-semibold text-lg">
                        {dataItem.totalValue.toLocaleString()}원
                    </p>
                    <p className={`text-sm font-medium ${dataItem.totalPnlRate >= 0 ? 'text-red-500' : 'text-blue-500'}`}>
                        수익률: {(dataItem.totalPnlRate * 100).toFixed(2)}%
                    </p>
                </div>
            );
        }
        return null;
    };

    return (
        <Card className="shadow-sm border-none bg-white w-full mt-6">
            <CardHeader className="border-b pb-4">
                <CardTitle className="text-lg">자산 추이 (최근 30일)</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={historyData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
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
                            tickFormatter={(value) => `${(value / 10000).toLocaleString()}만`}
                            tick={{ fontSize: 12, fill: '#6b7280' }}
                            axisLine={false}
                            tickLine={false}
                            width={60}
                            domain={['dataMin - 500000', 'dataMax + 500000']}
                        />

                        <Tooltip content={<CustomTooltip />} />

                        {/* 총 매수 금액 가로선 추가 */}
                        <ReferenceLine
                            y={totalInvested}
                            stroke="#10b981"
                            strokeDasharray="7 7"
                            strokeWidth={2}
                        >
                            <Label
                                value="매수금액"
                                position="top"
                                fill="#10b981"
                                fontSize={12}
                                className="font-medium"
                                offset={10}
                            />
                        </ReferenceLine>

                        <Line
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