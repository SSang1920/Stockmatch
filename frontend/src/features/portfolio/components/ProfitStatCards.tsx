import { Card, CardContent } from "@/components/ui/card";
import { BarChart3, Calendar, Receipt, TrendingDown, TrendingUp, Wallet } from "lucide-react";
import React from "react";

interface StatItemProps {
    title: string;
    value: number;
    rate?: number;
    icon: React.ReactNode;
}

function StatItem({ title, value, rate, icon }: StatItemProps) {
    const isPositive = value >= 0;
    const colorClass = isPositive ? "text-red-500" : "text-blue-500";
    const bgClass = isPositive ? "bg-red-50" : "bg-blue-50";

    return (
        <Card className="border-none shadow-sm bg-white overflow-hidden">
            <CardContent className="p-5">
                <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-gray-500">{title}</span>
                    <div className={`p-2 rounded-lg ${bgClass} ${colorClass}`}>
                        {icon}
                    </div>
                </div>
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

export function ProfitStatCards({ stats }: { stats: any }) {
    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <StatItem
                title="총 수익"
                value={stats?.totalProfit || 0}
                rate={stats?.totalRate}
                icon={<Wallet className="w-4 h-4" />}
            />
            <StatItem
                title="월간 수익"
                value={stats?.monthlyProfit || 0}
                rate={stats?.monthlyRate}
                icon={<Calendar className="w-4 h-4" />}
            />
            <StatItem
                title="연간 수익"
                value={stats?.annualProfit || 0}
                rate={stats?.annualRate}
                icon={<BarChart3 className="w-4 h-4" />}
            />
            <StatItem
                title="실현 손익"
                value={stats?.realizedProfit || 0}
                icon={<Receipt className="w-4 h-4" />}
            />
        </div>
    );
}