// 지수 정보 타입 (코스피, 나스닥, S&P 500)
export interface IndexInfo {
    name: string;
    price: number;
    change: number;
    changeRate: number;
}

// 환율 정보 타입 (달러/원)
export interface ExchangeRateInfo {
    name: string;
    rate: number;
    change: number;
    changeRate: number;
}

export interface MarketOverviewResponse {
    kospi: IndexInfo;
    nasdaq: IndexInfo;
    sp500: IndexInfo;
    usdKrw: ExchangeRateInfo;
    lastUpdateTime: string;
}