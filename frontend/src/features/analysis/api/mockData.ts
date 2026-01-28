import { AiResponseDto } from '../types';

export const MOCK_ANALYSIS_RESULT: AiResponseDto = {

    conclusionCode : 'COMPLEMENTARY',

    oneLineReview : '성장성과 안정성을 모두 갖춘 포트폴리오의 핵심 종목입니다.',

    reasons : [
        '현재 포트폴리오가 변동성이 높은 소형주 위주로 구성되어 있어, 애플의 강력한 현금 흐름이 전체 리스크를 효과적으로 낮춰줄 것으로 보입니다.',
        '최근 중국 시장 내 판매량 감소 우려가 있으나, 서비스 부문 매출 성장세가 이를 충분히 상쇄하고 있습니다.',
        '다만, 이미 기술주 비중이 60% 이상이라면 섹터 편중 리스크를 고려하여 비중을 조절할 필요가 있습니다.'
        ],

    disclaimer : '이 정보는 AI분석 결과이며 투자의 책임은 전적으로 본인에게 있습니다.'
    };