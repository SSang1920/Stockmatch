export interface AiRecommendation {
    type: 'STRONGLY_RECOMMENDED' | 'RECOMMENDED' | 'NEUTRAL' | 'NOT_RECOMMENDED' | 'WARNING';
    title: string; // 한줄 요약
    reasoning: string; // 이유
    riskFactors: string[]; // 리스크
    references: string[]; // 참조내용
}



export interface AnalysisResponse {
    ticker: string;
    recommendation: AiRecommendation;
}