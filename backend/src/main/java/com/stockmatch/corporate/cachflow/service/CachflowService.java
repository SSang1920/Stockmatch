package com.stockmatch.corporate.cachflow.service;

import com.stockmatch.corporate.cachflow.dto.CashflowDto;
import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.common.infra.GenericAlphaVantageClient;
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
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    public CashflowDto getCachflow(Long userId, String symbol){

        return cacheService.getOrLoad(
                function,
                symbol,
                CashflowDto.class,
                CACHE_TTL,
                () -> {
                    String apiKey = memberService.getDecryptedApiKey(userId);

                    return apiClient.fetchData(
                            function,
                            symbol,
                            apiKey,
                            CashflowDto.class
                    );
                }
        );
    }
}
