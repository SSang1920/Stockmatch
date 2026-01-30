// 종목 검색 결과
export interface StockSearchResponse {
    id: number;
    ticker: string;
    name: string;
    englishName: string;
    market: string;
    exchange: string;
}

export interface StockDetailResponse {
    ticker: string;
    name?: string;
    currentPrice: number;
    changeAmount: number;
    changeRate: number;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    volume: number;
    previousClose: number;
}