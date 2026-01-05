package com.stockmatch.corporate.global.earnings.service;

import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.global.common.infra.GenericAlphaVantageClient;
import com.stockmatch.corporate.global.earnings.dto.EarningsDto;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsService {

    private final GenericCacheService cacheService;
    private final GenericAlphaVantageClient apiClient;
    private final MemberService memberService;

    private static final String function = "earnings";
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    public EarningsDto getEarnings(Long userId, String symbol) {

        return cacheService.getOrLoad(
                function,
                symbol,
                EarningsDto.class,
                CACHE_TTL,
                () -> {
                    String apiKey = memberService.getDecryptedApiKey(userId);

                    return apiClient.fetchData(
                            function,
                            symbol,
                            apiKey,
                            EarningsDto.class
                    );
                }
        );

    }
}
