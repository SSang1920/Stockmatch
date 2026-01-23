package com.stockmatch.corporate.global.cashflow.service;

import com.stockmatch.corporate.global.cashflow.dto.CashflowDto;
import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.global.common.infra.GenericAlphaVantageClient;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CachflowService {

    private final GenericCacheService cacheService;
    private final GenericAlphaVantageClient apiClient;
    private final MemberService memberService;

    private static final String function = "cash_flow";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    public CashflowDto getCachflow(Long userId, String symbol){

        return cacheService.getOrLoad(
                function,
                symbol,
                CashflowDto.class,
                CACHE_TTL,
                () -> {
                    String apiKey = memberService.getDecryptedApiKey(userId);

                    waitApiLimit();
                    return apiClient.fetchData(
                            function,
                            symbol,
                            apiKey,
                            CashflowDto.class
                    );
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
