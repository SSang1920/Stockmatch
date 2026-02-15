import { useState } from "react";
import { HoldingItem, PortfolioSummary as SummaryType } from "./types";
import { PortfolioSummary } from "./components/PortfolioSummary";
import { HoldingsTable } from "./components/HoldingsTable";

// Mock Data
const MOCK_SUMMARY: SummaryType = {
  totalAsset: 15450000,
  totalInvested: 13000000,
  totalProfit: 2450000,
  totalProfitRate: 18.8,
  cashBalance: 500000,
};

const MOCK_HOLDINGS: HoldingItem[] = [
  { ticker: "AAPL", name: "Apple Inc.", krName: "애플", quantity: 10, avgPrice: 180.5, currentPrice: 225.0, currency: "USD" },
  { ticker: "005930", name: "삼성전자", quantity: 50, avgPrice: 68000, currentPrice: 74500, currency: "KRW" },
  { ticker: "TSLA", name: "Tesla, Inc.", krName: "테슬라", quantity: 5, avgPrice: 210.0, currentPrice: 198.5, currency: "USD" },
];

export default function PortfolioPage() {
    // Mock 데이터 사용
    const [summary] = useState<SummaryType>(MOCK_SUMMARY);
    const [holdings] = useState<HoldingItem[]>(MOCK_HOLDINGS);
    const exchangeRate = 1450;

    return (
        <div className="p-6 space-y-6 max-w-5xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800">내 포트폴리오</h1>
      
      {/* 요약 카드 섹션 */}
      <PortfolioSummary summary={summary} />

      {/* 상세 내역 섹션 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* 왼쪽: 보유 종목 (2칸 차지) */}
        <div className="md:col-span-2">
           <HoldingsTable holdings={holdings} rate={exchangeRate} />
        </div>

        {/* 오른쪽: 차트나 기타 정보 (1칸 차지) */}
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-xl shadow-sm border h-64 flex items-center justify-center text-gray-400">
            📊 자산 배분 차트 (준비중)
          </div>
        </div>
      </div>
    </div>
    )
}