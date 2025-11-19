package com.stockmatch.corporate.overview.service;

import com.stockmatch.corporate.overview.cache.OverviewCacheService;
import com.stockmatch.corporate.overview.dto.CompanyOverviewDto;
import com.stockmatch.corporate.overview.infra.AlphaVantageOverviewClient;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverviewService {

    private final OverviewCacheService cacheService;
    private final AlphaVantageOverviewClient alphaVantageClient;
    private final MemberService memberService;

    public CompanyOverviewDto getCompanyOverview(Long userId, String symbol) {

        return cacheService.getOrLoadOverview(symbol, () -> {

            // 사용자 API 키를 가져오기
            String apiKey = memberService.getDecryptedApiKey(userId);
            // API 클라이언트를 호출
            return alphaVantageClient.getCompanyOverview(symbol, apiKey);
        });
    }
}
