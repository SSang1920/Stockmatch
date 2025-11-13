package com.stockmatch.financials.service;

import com.stockmatch.financials.cache.FinancialsCacheService;
import com.stockmatch.financials.dto.CompanyOverviewResponse;
import com.stockmatch.financials.infra.AlphaVantageClient;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialsCacheService cacheService;
    private final AlphaVantageClient alphaVantageClient;
    private final MemberService memberService;

    public CompanyOverviewResponse getCompanyOverview(Long userId, String symbol) {

        return cacheService.getOrLoadOverview(symbol, () -> {

            // 사용자 API 키를 가져오기
            String apiKey = memberService.getDecryptedApiKey(userId);
            // API 클라이언트를 호출
            return alphaVantageClient.getCompanyOverview(symbol, apiKey);
        });
    }
}
