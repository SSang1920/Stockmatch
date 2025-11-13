package com.stockmatch.financials.client;

import com.stockmatch.financials.dto.CompanyOverviewResponse;

public interface ExternalFinancialsClient {

    /**
     * 외부 API를 통해 기업 개요 정보 조회
     * @param symbol 기업의 티커
     * @param apiKey 사용자의 API키
     * @return CompanyOverviewResponse DTO
     */
    CompanyOverviewResponse getCompanyOverview(String symbol, String apiKey);
}
