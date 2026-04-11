import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { BarChart3, CalendarDays, Clock, Receipt, TrendingDown, TrendingUp, Wallet } from "lucide-react";
import React, { useMemo } from "react";

interface StatItemProps {
    title: string;
    value: number;
    rate?: number;
    icon: React.ReactNode;
    selector?: React.ReactNode;
}

function StatCard({ title, value, rate, icon, selector }: StatItemProps) {
    const isPositive = value >= 0;
    const colorClass = isPositive ? "text-red-500" : "text-blue-500";
    const bgClass = isPositive ? "bg-red-50" : "bg-blue-50";

    return (
        <Card className="border-none shadow-sm bg-white min-h-[140px] flex flex-col justify-between">
            <CardHeader className="p-5 pb-2 flex flex-row items-center justify-between space-y-0">
                <div className="flex items-center gap-2">
                    <div className={`p-2 rounded-lg ${bgClass} ${colorClass}`}>
                        {icon}
                    </div>
                    <CardTitle className="text-sm font-medium text-gray-500">{title}</CardTitle>
                </div>
                {selector}
            </CardHeader>
            <CardContent className="p-5 pt-0">
                <div className="flex flex-col">
                    <span className="text-2xl font-bold text-gray-800">
                        {isPositive ? "" : "-"}{Math.abs(value).toLocaleString()}
                        <span className="text-sm font-normal text-gray-500 ml-1">원</span>
                    </span>
                    {rate !== undefined && (
                        <div className={`flex items-center text-sm font-semibold mt-1 ${colorClass}`}>
                            {isPositive ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
                            {Math.abs(rate).toFixed(2)}%
                        </div>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}

export function ProfitStatCards({ stats, userCreatedAt, selectedYear, setSelectedYear, selectedMonth, setSelectedMonth }: any) {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;

    const signupDate = userCreatedAt ? new Date(userCreatedAt) : new Date();
    const signupYear = signupDate.getFullYear();
    const signupMonth = signupDate.getMonth() + 1;

    // 연도 리스트 생성
    const availableYears = useMemo(() => {
        const years = [];
        for (let y = currentYear; y >= signupYear; y--) {
            years.push(y.toString());
        }
        return years;
    }, [currentYear, signupYear]);

    // 월 리스트 생성
    const months = Array.from({ length: 12 }, (_, i) => (i + 1).toString());

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            {/* 누적 수익 (전체 기간) */}
            <StatCard
                title="누적 수익"
                value={stats?.realizedProfit ?? stats?.totalProfit ?? 0}
                rate={stats?.totalRate}
                icon={<Wallet className="w-4 h-4" />}
            />

            {/* 연간 수익 */}
            <StatCard
                title="연간 수익"
                value={stats?.annualProfit ?? 0}
                rate={stats?.annualRate}
                icon={<BarChart3 className="w-4 h-4" />}
                selector={
                    <Select value={selectedYear} onValueChange={setSelectedYear}>
                        <SelectTrigger className="w-[90px] h-8 text-xs border-none bg-gray-50">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {availableYears.map(year => (
                                <SelectItem key={year} value={year}>{year}년</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                }
            />

            {/* 월간 수익 */}
            <StatCard
                title="월간 수익"
                value={stats?.monthlyProfit ?? 0}
                rate={stats?.monthlyRate}
                icon={<CalendarDays className="w-4 h-4" />}
                selector={
                    <Select value={selectedMonth} onValueChange={setSelectedMonth}>
                        <SelectTrigger className="w-[80px] h-8 text-xs border-none bg-gray-50">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {months.map(m => (
                                <SelectItem key={m} value={m}>{m}월</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                }
            />

            {/* 일간 수익 */}
            <StatCard
                title="일간 수익"
                value={stats?.dailyProfit ?? 0}
                rate={stats?.dailyRate}
                icon={<Clock className="w-4 h-4" />}
            />
        </div>
    );
}