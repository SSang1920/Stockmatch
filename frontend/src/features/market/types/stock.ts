// 종목 검색 결과
export interface StockSearchResponse {
    id: number;
    ticker: string;
    name: string;
    englishName: string;
    market: string;
    exchange: string;
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

// 시장 트렌드 API 전체 응답 구조
export interface MarketTrendResponse {
    mostActive: StockTrend[];
    gainers: StockTrend[];
    losers: StockTrend[];
}