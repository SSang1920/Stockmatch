package com.stockmatch.corporate.overview.client;

import com.stockmatch.corporate.overview.dto.CompanyOverviewDto;

public interface ExternalOverviewClient {

    /**
     * 외부 API를 통해 기업 개요 정보 조회
     * @param symbol 기업의 티커
     * @param apiKey 사용자의 API키
     * @return CompanyOverviewResponse DTO
     */
    CompanyOverviewDto getCompanyOverview(String symbol, String apiKey);
}
