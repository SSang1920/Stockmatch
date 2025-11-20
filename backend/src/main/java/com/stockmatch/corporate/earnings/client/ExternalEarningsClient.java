package com.stockmatch.corporate.earnings.client;

import com.stockmatch.corporate.earnings.dto.EarningsDto;

public interface ExternalEarningsClient {

    /**
     * 외부 API를 통해 기업의과거 실적 정보 조회
     * @param symbol 기업의 티커
     * @param apiKey 사용자의 API키
     * @return EarningsDTO
     */
    EarningsDto getEarnings(String symbol, String apiKey);
}
