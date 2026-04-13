import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { HoldingItem } from "../types";
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

interface PortfolioDonutChartProps {
    holdings: HoldingItem[];
    className?: string;
}

const COLORS = [
    '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
    '#ec4899', '#14b8a6', '#f97316', '#06b6d4', '#84cc16'
];

const OTHER_COLOR = '#cbd5e1';

// 비중 5% 미만 기타 처리
const MIN_PERCENTAGE = 0.05;

export function PortfolioDonutChart({ holdings, className }: PortfolioDonutChartProps) {
    // 전체 자산 총액 계산
    const totalValue = holdings.reduce((sum, item) => sum + Math.max(0, item.value || 0), 0);

    const mainItems: { name: string, value: number }[] = [];
    let othersSum = 0;

    holdings
        .filter(item => item.value > 0)
        .map((item) => ({
            name: item.krName || item.name || item.ticker,
            value: Math.floor(item.value || 0),
        }))
        .sort((a, b) => b.value - a.value)
        .forEach((item) => {
            const weight = item.value / totalValue;

            if (weight >= MIN_PERCENTAGE) {
                mainItems.push(item);
            } else {
                othersSum += item.value;
            }
        });

    if (othersSum > 0) {
        mainItems.push({ name: "기타", value: othersSum });
    }

    // 데이터가 아예 없을 때의 화면
    if (totalValue === 0 || mainItems.length === 0) {
        return (
            <Card className="shadow-sm border-none bg-white h-full">
                <CardHeader className="border-b pb-4">
                    <CardTitle className="text-lg">자산 비중</CardTitle>
                </CardHeader>
                <CardContent className="flex items-center justify-center h-[300px] text-gray-400">
                    보유 중인 자산이 없습니다.
                </CardContent>
            </Card>
        );
    }

    // 커스텀 툴팁
    const CustomTooltip = ({ active, payload }: any) => {
        if (active && payload && payload.length) {
            const dataItem = payload[0];
            const percent = ((dataItem.value / totalValue) * 100).toFixed(1); // 비중 % 계산

            return (
                <div className="bg-white border border-gray-200 p-3 rounded-lg shadow-md z-50">
                    <p className="font-bold text-gray-800">{dataItem.name}</p>
                    <div className="flex items-center gap-2 mt-1">
                        <span className="text-blue-600 font-semibold">
                            {dataItem.value.toLocaleString()}원
                        </span>
                        <span className="text-xs text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded">
                            {percent}%
                        </span>
                    </div>
                </div>
            );
        }
        return null;
    };

    return (
        <Card className={`shadow-sm border-none bg-white flex flex-col h-full ${className}`}>
            <CardHeader className="border-b pb-4">
                <CardTitle className="text-lg">자산 비중</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 h-[350px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                        <Pie
                            data={mainItems}
                            cx="50%"
                            cy="45%"
                            innerRadius={70}
                            outerRadius={100}
                            paddingAngle={2}
                            dataKey="value"
                            stroke="none"
                        >
                            {mainItems.map((entry, index) => (
                                <Cell
                                    key={`cell-${index}`}
                                    fill={entry.name === "기타" ? OTHER_COLOR : COLORS[index % COLORS.length]}
                                />
                            ))}
                        </Pie>
                        <Tooltip content={<CustomTooltip />} />
                        <Legend
                            verticalAlign="bottom"
                            height={36}
                            iconType="circle"
                            formatter={(value) => <span className="text-sm text-gray-600">{value}</span>}
                        />
                    </PieChart>
                </ResponsiveContainer>
            </CardContent>
        </Card>
    );
}