package com.stockmatch.corporate.analysis.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockmatch.corporate.analysis.dto.data.UserContext;
import lombok.*;

import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiResponseDto {

    // 결론 (구분용)
    private ConclusionCode  conclusionCode;

    // 결론 문장 (사용자 보는용)
    private String oneLineReview;

    // 판단 이유
    private String detailedAnalysis;

    //주의 문구 (예시 : 보유 종목의 성격은 티커 기반 추정이므로 틀릴 수 있습니다.)
    private String disclaimer;

    // 투자 비중 //포트폴리오 분석떄만 값이오고 나머지에선 null
    private List<UserContext.AiPortfolioHoldingDto> currentHoldings;

    public enum ConclusionCode {
        COMPLEMENTARY, // 보완/긍정 포트폴리오 약점 보완 or 밸런스 맞춰줌
        NEUTRAL, // 중립 기존 성향과 비슷하거나 무난
        BURDEN, // 부담 주의 리스크를 키우거나 특정 섹터 쏠림 심화

        WELL_BALANCED, // 분산 투자가 잘됨
        CONCENTRATED, // 특정 섹터나 종목에 비중이 몰림
        HIGH_RISK, // 위험도가 큰 포트폴리오

        ERROR
    }
}
