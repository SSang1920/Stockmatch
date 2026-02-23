export interface HoldingItem {
    holdingId: number;
    ticker: string;
    name: string;           // 종목명
    krName?: string;        // 한글 종목명
    quantity: number;       // 보유 수량
    avgPrice: number;       // 매입 평균가
    currentPrice: number;   // 현재가
    valuation: number;      // 평가금액
    profit: number;         // 손익금액
    returnRate: number;     // 수익률
    currency: 'KRW' | 'USD';
}

export interface PortfolioValuationResponse {
    totalAsset: number;             // 총 자산
    totalPurchaseAmount: number;    // 총 매수 금액
    totalProfit: number;            // 총 손익금액
    totalReturnRate: number;        // 총 수익률
    dailyChange: number;            // 전일 대비 변도액
    dailyChangeRate: number;        // 전일 대비 변동률
    exchangeRate: number;           // 환율
    holdings: HoldingItem[];        // 보유 종목 리스트
}

// 일별 차트 데이터 타입
export interface PortfolioDailySummaryResponse {
    date: string;
    totalAsset: number;
    totalReturnRate: number;
}