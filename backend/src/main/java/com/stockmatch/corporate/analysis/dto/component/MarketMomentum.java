package com.stockmatch.corporate.analysis.dto.component;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MarketMomentum {

    private String sector; //종목 섹터
    private String industry;
    private String market;

    // 현재 가치 평가 지표 , AI가 업종 평균과 대조해볼때 사용
    private Double peRatio;
    private Double forwardPeRatio;
    private Double psRatio;

    // 뉴스 및 실적
    private Double newsSentimentScore;    // alphaVantage 뉴스 점수 AI가 분석한 점수 -1.0 ~ 1.0
    private String newsSummary;
    private Double lastEarningsSurpriseRatio;

}
