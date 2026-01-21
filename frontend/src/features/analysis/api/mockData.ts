import { AnalysisResponse } from '../types';

export const MOCK_ANALYSIS_RESULT: AnalysisResponse = {
    ticker: 'AAPL',
    recommendation: {
        type: 'STRONGLY_RECOMMENDED',
        title: '성장성과 안정성을 모두 갖춘 포트폴리오의 핵심',
        reasoning: '회원님의 포트폴리오는 변동성이 높은 소형주 위주로 구성되어 있습니다. AAPL은 강력한 현금 흐름과 안정적인 배당을 통해 전체 포트폴리오의 리스크를 낮춰줄 수 있는 최적의 선택입니다.',
        riskFactors: [
            '최근 중국 시장 내 아이폰 판매량 감소 추세',
            '이미 기술주 비중이 60% 이상일 경우 섹터 편중 주의'
            ],
        references: [
            '사용자 포트폴리오',
            '최근 4분기 현금 흐름표',
            '배당 지급 내역'
            ]
        }
    };