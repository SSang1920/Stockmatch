package com.stockmatch.user.member.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UserInvestmentType {

    STABLE("안정형", "원금 보존을 최우선으로 하는 투자자", 1),

    CAUTIOUS("안정추구형", "원금 손실을 최소화하며 예적금보다 높은 수익 추구", 2),

    BALANCED("위험중립형", "일정 수준의 손실을 감수하고 수익을 추구", 3),

    GROWTH("적극투자형", "높은 수익을 위해 주식 비중을 높게 가져감", 4),

    AGGRESSIVE("공격투자형", "큰 변동성을 감수하고 고수익을 추구", 5);

    @JsonValue
    private final String description;

    private final String detail;
    private final int riskLevel;

    // 점수 기반 투자 성향 분류
    public static UserInvestmentType findByScore(int score) {
        if(score <= 15) return STABLE;
        if(score <= 22) return CAUTIOUS;
        if(score <= 29) return BALANCED;
        if(score <= 36) return GROWTH;
        return AGGRESSIVE;
    }

    // 위험도 레벨로 찾기
    public static UserInvestmentType findByRiskLevel(int riskLevel) {
        return Arrays.stream(values())
                .filter(type -> type.riskLevel == riskLevel)
                .findFirst()
                .orElse(STABLE);
    }
}
