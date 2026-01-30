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

// 시장 트렌드/랭킹 주식 정보
export interface StockTrend {
    ticker: string;
    name: string;
    price: string;
    change: string;
    changeRate: string;
    market: string;
}

// 시장별 트렌드 데이터 구조
export interface MarketTrendData {
    mostActive: StockTrend[];
    gainers: StockTrend[];
    losers: StockTrend[];
}

// 시장 트렌드 API 전체 응답 구조
export interface MarketTrendResponse {
    KR: MarketTrendData;
    US: MarketTrendData;
}