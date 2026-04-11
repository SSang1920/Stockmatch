/**
 * 공통 응답
 */
interface BaseAnalysisResponse {
    conclusionCode: string; // 예: 'POSITIVE', 'CAUTION', 'NEUTRAL'
    oneLineReview: string;  // AI의 핵심 요약 한 줄
    detailedAnalysis: string; // 상세 분석 리포트
    disclaimer: string;     // 법적 면책 조항
}

// 내 포트폴리오 기반종목 추가 적합 여부
export interface StockSuitabilityResponse extends BaseAnalysisResponse {
}

// 내 포트폴리오 분석 및 추천
export interface PortfolioAnalysisResponse extends BaseAnalysisResponse {
    //현재 내 포트폴리오 구성 (이름 + 비중)
    currentComposition: {
            name: string;   // 예: 'IT/반도체'
            weight: number; // 예: 75
        }[];
}

// 재무제표 분석
export interface FinancialAnalysisResponse extends BaseAnalysisResponse {

}

export interface ApiResponse<T> {
    status: string;
    message: string | null;
    data : T;
}

export interface AnalysisHistoryListResponse {
    id: number;
    symbol: string;
    analyzedAt: string;
    type: 'STOCK' | 'PORTFOLIO' | 'FINANCIAL';
}