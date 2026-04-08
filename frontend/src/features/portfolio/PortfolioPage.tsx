import { useState } from "react";
import { HoldingsTable } from "./components/HoldingsTable";
import { PortfolioSummary } from "./components/PortfolioSummary";
import { usePortfolioValuation } from "./hooks/usePortfolio";
import { AlertCircle, Loader2, Plus } from "lucide-react";
import { HoldingItem } from "./types";
import { Button } from "@/components/ui/button";
import { PortfolioDonutChart } from "./components/PortfolioDonutChart";
import { PortfolioLineChart } from "./components/PortfolioLineChart";
import { UnifiedTradeModal } from "./components/UnifiedTradeModal";
import { ProfitStatCards } from "./components/ProfitStatCards";

export default function PortfolioPage() {
  const { data: valuation, isLoading, error } = usePortfolioValuation();
  const { data: valuationData } = usePortfolioValuation();

  const dummyStats = {
    totalProfit: 1250000,
    totalRate: 8.5,
    monthlyProfit: 350000,
    monthlyRate: 2.1,
    annualProfit: -120000,
    annualRate: -0.8,
    realizedProfit: 450000
  };

  // 모달 상태 관리
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [selectedHolding, setSelectedHolding] = useState<HoldingItem | null>(null);


  // 종목 추가 열기
  const handleOpenAdd = () => {
    setSelectedHolding(null);
    setIsFormModalOpen(true);
  };

  // 종목 수정 열기
  const handleOpenEdit = (holding: HoldingItem) => {
    setSelectedHolding(holding);
    setIsFormModalOpen(true);
  };

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
      <div className="flex items-end justify-between">
        <div className="flex items-baseline gap-3">
          <h1 className="text-2xl font-bold text-gray-800">내 포트폴리오</h1>
          <span className="text-xs text-gray-500 font-medium bg-gray-100 px-2.5 py-1 rounded-md border border-gray-200">
            적용 환율: ₩{(valuation.usdToKrwRate || 0).toLocaleString()}
          </span>
        </div>
        <Button size="sm" onClick={handleOpenAdd} className="bg-gray-900 text-white hover:bg-gray-800 shadow-sm transition-all">
          <Plus className="h-4 w-4 mr-1" /> 종목 추가
        </Button>
      </div>

      {/* 요약 카드 섹션 */}
      <PortfolioSummary
        totalValue={valuation.totalValue}
        totalInvested={valuation.totalInvested}
        totalPnlAmount={valuation.totalPnlAmount}
        totalPnlRate={valuation.totalPnlRate}
      />

      <ProfitStatCards stats={dummyStats} />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* 왼쪽: 보유 종목 */}
        <div className="md:col-span-2">
          <HoldingsTable
            holdings={valuation.holdings}
            usdToKrwRate={valuation.usdToKrwRate}
            onEdit={handleOpenEdit}
          />
        </div>

        {/* 오른쪽: 차트 섹션 */}
        <div className="space-y-4">
          <PortfolioDonutChart holdings={valuation.holdings} />
        </div>
      </div>

      {/* 일별 자산 추이 차트 */}
      <PortfolioLineChart />

      {/* 추가/수정 모달 컴포넌트 */}
      <UnifiedTradeModal
        portfolioId={valuation.portfolioId}
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        holdingToEdit={selectedHolding}
      />
    </div>
  );
}