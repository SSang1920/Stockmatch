import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useDailyHistory, usePortfolioValuation } from "../hooks/usePortfolio";
import { Loader2 } from "lucide-react";
import { CartesianGrid, Label, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export function PortfolioLineChart() {
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
            return (
                <div className="bg-white border border-gray-200 p-3 rounded-lg shadow-md z-50">
                    <p className="font-bold text-gray-600 mb-1">{label}</p>
                    <p className="text-blue-600 font-semibold text-lg">
                        {dataItem.totalValue.toLocaleString()}원
                    </p>
                    <p className={`text-sm font-medium ${dataItem.totalRate >= 0 ? 'text-red-500' : 'text-blue-500'}`}>
                        수익률: {(dataItem.totalRate * 100).toFixed(2)}%
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