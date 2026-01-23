package com.stockmatch.corporate.global.overview.service;

import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.global.common.infra.GenericAlphaVantageClient;
import com.stockmatch.corporate.global.overview.dto.CompanyOverviewDto;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverviewService {

    private final GenericCacheService cacheService;
    private final GenericAlphaVantageClient apiClient;
    private final MemberService memberService;

    private static final String function = "overview";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    public CompanyOverviewDto getCompanyOverview(Long userId, String symbol) {

        return cacheService.getOrLoad(
                function,
                symbol,
                CompanyOverviewDto.class,
                CACHE_TTL,
                () -> {
                    String apiKey = memberService.getDecryptedApiKey(userId);

                    waitApiLimit();
                    return apiClient.fetchData(
                            function,
                            symbol,
                            apiKey,
                            CompanyOverviewDto.class);
                }
        );
    }

    private void waitApiLimit() {
        try {
            log.info("API 속도 제한을 위해 13초 대기중");
            Thread.sleep(13000);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
