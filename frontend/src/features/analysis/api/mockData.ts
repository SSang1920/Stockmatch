import { StockSuitabilityResponse,
           PortfolioAnalysisResponse,
           FinancialAnalysisResponse
           } from '../types';

// 종목 추가 적합 여부
export const MOCK_ANALYSIS_RESULT: StockSuitabilityResponse = {

    conclusionCode : 'COMPLEMENTARY',

    oneLineReview : '성장성과 안정성을 모두 갖춘 포트폴리오의 핵심 종목입니다.',

    detailedAnalysis:
        "현재 포트폴리오가 변동성이 높은 소형주 위주로 구성되어 있어, 애플의 강력한 현금 흐름이 전체 리스크를 효과적으로 낮춰줄 것으로 보입니다.\n\n" +
        "최근 중국 시장 내 판매량 감소 우려가 있으나, 서비스 부문 매출 성장세가 이를 충분히 상쇄하고 있습니다.\n\n" +
        "다만, 이미 기술주 비중이 60% 이상이라면 섹터 편중 리스크를 고려하여 비중을 조절할 필요가 있습니다.",

    disclaimer : '이 정보는 AI분석 결과이며 투자의 책임은 전적으로 본인에게 있습니다.'
    };

// 포트폴리오 분석 및 추천
export const MOCK_PORTFOLIO_ANALYSIS: PortfolioAnalysisResponse = {
  conclusionCode: 'CAUTION',
  oneLineReview: '기술주 비중이 과도하게 높아 하락장 대비가 필요한 상태입니다.',

  //현재 포트폴리오 요약
  currentHoldings: [
      { name: 'IT/반도체', weightPct: 75 },
      { name: '2차전지', weightPct: 15 },
      { name: '현금/안전매물', weightPct: 5 },
      { name: '기타', weightPct: 5 }
    ],

  detailedAnalysis: `현재 포트폴리오는 나스닥 대형 기술주와 국내 IT 종목이 전체 자산의 80% 이상을 차지하고 있습니다.\n\n최근처럼 금리 변동성이 큰 시기에는 기술주 중심의 포트폴리오가 시장보다 더 크게 흔들릴 가능성이 높습니다.\n\n
  추천 전략으로는 수익이 난 종목을 일부 실현하여 현금 비중을 15%까지 확보하거나, 기술주와 상관관계가 낮은 금(Gold) 또는 채권형 자산을 추가하는 것이 좋습니다.`,
  disclaimer: '이 정보는 AI분석 결과이며 투자의 책임은 전적으로 본인에게 있습니다.'
};

// 재무제표 분석
export const MOCK_FINANCIAL_ANALYSIS: FinancialAnalysisResponse = {
  conclusionCode: 'POSITIVE',
  oneLineReview: '수익성 개선 지표가 뚜렷하며 재무 건전성이 매우 우수한 기업입니다.',
  title: '실적 개선 청신호',
  detailedAnalysis: `
    이 기업의 가장 긍정적인 신호는 최근 3분기 연속 영업이익률이 계단식으로 상승하고 있다는 점입니다.
    부채 비율은 40% 미만으로 업종 평균 대비 매우 낮으며, 유보율이 높아 향후 신사업 투자나 배당 확대의 여력이 충분해 보입니다.
    재무제표상으로만 본다면 기초 체력이 아주 튼튼한 상태이며, 현재 주가 수준은 미래 성장 가치를 충분히 반영하지 못한 저평가 구간으로 판단됩니다.
    안정적인 투자를 선호하신다면 장기 보유하기에 적합한 재무 구조를 갖추고 있습니다.
  `,
  disclaimer: '이 정보는 AI분석 결과이며 투자의 책임은 전적으로 본인에게 있습니다.'
};