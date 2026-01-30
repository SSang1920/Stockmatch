export interface AiResponseDto {

    // 결론 코드
    conclusionCode : string;

    // 한 줄 요약
    oneLineReview : string;

    //판단 근거 리스트
    reasons : string[];

    // 면책 조항
    disclaimer: string;
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
}