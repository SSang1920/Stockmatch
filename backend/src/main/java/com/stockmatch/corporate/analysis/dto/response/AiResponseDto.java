package com.stockmatch.corporate.analysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {

    // 결론 (구분용)
    private ConclusionCode  conclusionCode;

    // 결론 문장 (사용자 보는용)
    private String oneLineReview;

    // 판단 이유
    private List<String> reasons;

    //주의 문구 (예시 : 보유 종목의 성격은 티커 기반 추정이므로 틀릴 수 있습니다.)
    private String disclaimer;

    public enum ConclusionCode {
        COMPLEMENTARY, // 보완/긍정 포트폴리오 약점 보완 or 밸런스 맞춰줌
        NEUTRAL, // 중립 기존 성향과 비슷하거나 무난
        BURDEN // 부담 주의 리스크를 키우거나 특정 섹터 쏠림 심화
    }
}
