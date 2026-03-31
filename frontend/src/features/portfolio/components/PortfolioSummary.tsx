import { Card, CardContent } from "@/components/ui/card";
import { ArrowDownRight, ArrowUpRight, TrendingUp } from "lucide-react";

const formatKrw = (val: number) => {
  const num = Number(val) || 0;
  return new Intl.NumberFormat('ko-KR').format(Math.floor(num)) + '원';
};

interface Props {
    totalValue: number;
    totalInvested: number;
    totalPnlAmount: number;
    totalPnlRate: number;
}

export function PortfolioSummary({ totalValue, totalInvested, totalPnlAmount, totalPnlRate }: Props) {
    const isPlus = totalPnlAmount >= 0;
    const colorClass = isPlus ? "text-red-500" : "text-blue-500";

    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      {/* 총 자산 */}
      <Card className="border-none shadow-md bg-gradient-to-br from-gray-900 to-gray-800 text-white">
        <CardContent className="p-6 flex flex-col justify-between h-full">
          <div>
            <p className="text-gray-400 text-sm font-medium">총 자산 평가액</p>
            <h2 className="text-3xl font-bold mt-2">{formatKrw(totalValue)}</h2>
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm text-gray-300">
            <TrendingUp className="w-4 h-4" />
            <span>실시간 시세 반영됨</span>
          </div>
        </CardContent>
      </Card>

      {/* 손익 */}
      <Card className="border-none shadow-sm bg-white">
        <CardContent className="p-6 flex flex-col justify-center h-full">
          <p className="text-gray-500 text-sm font-medium">총 투자 손익</p>
          <div className={`text-2xl font-bold mt-2 flex items-center gap-1 ${colorClass}`}>
            {isPlus ? <ArrowUpRight className="w-6 h-6" /> : <ArrowDownRight className="w-6 h-6" />}
            {isPlus ? "+" : ""}{formatKrw(totalPnlAmount)}
          </div>
          <p className={`text-sm mt-1 font-medium ${colorClass}`}>
            {isPlus ? "+" : ""}{((totalPnlRate || 0) * 100).toFixed(2)}%
          </p>
        </CardContent>
      </Card>

      {/* 매수 금액 */}
      <Card className="border-none shadow-sm bg-white">
        <CardContent className="p-6 flex flex-col justify-center h-full">
          <p className="text-gray-500 text-sm font-medium">총 매수 금액</p>
          <h2 className="text-2xl font-bold mt-2 text-gray-800">{formatKrw(totalInvested)}</h2>
          <div className="mt-2 text-xs text-gray-400">
            * 환율 변동에 따라 오차가 있을 수 있습니다.
          </div>
        </CardContent>
      </Card>
    </div>
    )
}