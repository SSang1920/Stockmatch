export interface HoldingItem {
    ticker: string;
    name: string;           // 종목명
    quantity: number;       // 보유 수량
    avgPrice: number;       // 매입 평균가
    currentPrice: number;   // 현재가
    value: number;          // 평가금액
    pnlAmount: number;      // 손익금액
    pnlRate: number;        // 수익률
    invested: number;       // 매입금액

    holdingId: number;
    krName?: string;        // 한글 종목명
    currency: 'KRW' | 'USD';
}

export interface PortfolioValuationResponse {
    portfolioId: number;
    totalValue: number;             // 총 자산
    totalInvested: number;          // 총 매수 금액
    totalPnlAmount: number;         // 총 손익금액
    totalPnlRate: number;           // 총 수익률
    usdToKrwRate: number;           // 환율
    holdings: HoldingItem[];        // 보유 종목 리스트

    dailyChange: number;            // 전일 대비 변도액
    dailyChangeRate: number;        // 전일 대비 변동률
}

// 일별 차트 데이터 타입
export interface PortfolioDailySummaryResponse {
    date: string;
    totalValue: number;
    totalPnlRate: number;
}

export interface HoldingPayload {
    ticker: string;
    quantity: number;
    avgPrice: number;
};