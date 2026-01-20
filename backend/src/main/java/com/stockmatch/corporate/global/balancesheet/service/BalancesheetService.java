package com.stockmatch.corporate.global.balancesheet.service;

import com.stockmatch.corporate.global.balancesheet.dto.BalancesheetDto;
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
public class BalancesheetService {

    private final GenericCacheService cacheService;
    private final GenericAlphaVantageClient apiClient;
    private final MemberService memberService;

    private static final String function = "balance_sheet";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    public BalancesheetDto getBalancesheet(Long userId, String symbol){

        return cacheService.getOrLoad(
                function,
                symbol,
                BalancesheetDto.class,
                CACHE_TTL,
                () -> {
                    String apiKey = memberService.getDecryptedApiKey(userId);

                    return apiClient.fetchData(
                            function,
                            symbol,
                            apiKey,
                            BalancesheetDto.class
                    );
                }
        );
    }
}
