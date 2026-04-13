import { Card, CardContent } from "@/components/ui/card";
import { TrendingUp } from "lucide-react";

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
  const unrealizedPnL = Math.floor(totalValue - totalInvested);
  const unrealizedRate = totalInvested > 0 ? (unrealizedPnL / totalInvested) * 100 : 0;

  const isPlus = totalPnlAmount >= 0;
  const isUnrealizedPlus = unrealizedPnL >= 0;
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
          <div className={`flex items-center gap-1 mt-4 text-sm font-bold ${isUnrealizedPlus ? 'text-red-400' : 'text-blue-400'}`}>
            {unrealizedPnL >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingUp className="w-4 h-4 rotate-180" />}
            <span>
              {isUnrealizedPlus ? "+" : ""}{unrealizedPnL.toLocaleString()}원
              ({unrealizedRate.toFixed(2)}%)
            </span>
          </div>
        </CardContent>
      </Card>

      {/* 손익 */}
      <Card className="border-none shadow-sm bg-white">
        <CardContent className="p-6 flex flex-col justify-center h-full">
          <p className="text-gray-500 text-sm font-medium">총 투자 손익</p>
          <div className={`text-2xl font-bold mt-2 flex items-center gap-1 ${colorClass}`}>
            {isPlus ? <TrendingUp className="w-6 h-6" /> : <TrendingUp className="w-4 h-4 rotate-180" />}
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