import { HoldingsTable } from "./components/HoldingsTable";
import { PortfolioSummary } from "./components/PortfolioSummary";
import { usePortfolioValuation } from "./hooks/usePortfolio";
import { AlertCircle, Loader2 } from "lucide-react";

export default function PortfolioPage() {
  const { data: valuation, isLoading, error } = usePortfolioValuation();

  if (isLoading) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center space-y-4">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="text-muted-foreground animate-pulse">자산 정보를 불러오는 중입니다...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center p-6 text-center">
        <AlertCircle className="h-12 w-12 text-destructive mb-4" />
        <h2 className="text-xl font-semibold">데이터 조회 실패</h2>
        <p className="text-muted-foreground mt-2">{error instanceof Error ? error.message : "알 수 없는 에러가 발생했습니다."}</p>
      </div>
    );
  }

  if (!valuation) return null;

  return (
    <div className="p-6 space-y-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">내 포트폴리오</h1>
        <span className="text-xs text-muted-foreground">
          적용 환율: ₩{valuation.exchangeRate.toLocaleString()}
        </span>
      </div>

      {/* 요약 카드 섹션 */}
      <PortfolioSummary
        totalAsset={valuation.totalAsset}
        totalPurchaseAmount={valuation.totalPurchaseAmount}
        totalProfit={valuation.totalProfit}
        totalReturnRate={valuation.totalReturnRate}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* 왼쪽: 보유 종목 */}
        <div className="md:col-span-2">
          <HoldingsTable holdings={valuation.holdings} exchangeRate={valuation.exchangeRate}/>
        </div>

        {/* 오른쪽: 차트 섹션 */}
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-xl shadow-sm border h-64 flex flex-col items-center justify-center text-gray-400">
            <span className="text-3xl mb-2">📊</span>
            <p className="text-sm font-medium">자산 배분 차트</p>
            <p className="text-xs mt-1">준비중인 기능입니다.</p>
          </div>
        </div>
      </div>
    </div>
  );
}