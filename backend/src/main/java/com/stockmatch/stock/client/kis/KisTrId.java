package com.stockmatch.stock.client.kis;

public abstract class KisTrId {

    // ====== 국내 (KR) =====
    // 국내주식 현재가 시세
    public static final String KR_REAL_TIME = "FHKST01010100";
    // 국내주식 기간별 시세 (일봉)
    public static final String KR_DAILY_PRICE = "FHKST03010100";
    // 국내주식 현재지수
    public static final String KR_INDEX = "FHPUP02100000";
    // 국내주식 거래량 순위
    public static final String KR_RANK_VOLUME = "FHPST01710000";

    // ===== 해외 (US) =====
    // 해외주식 현재가 시세
    public static final String US_REAL_TIME = "HHDFS76200200";
    // 국내주식 기간별 시세 (일봉)
    public static final String US_DAILY_PRICE = "HHDFS76240000";
    // 국내주식 현재지수
    public static final String US_INDEX = "FHKST0303010";
    // 국내주식 거래량 순위
    public static final String US_RANK_VOLUME = "HHDFS76310010";
}
