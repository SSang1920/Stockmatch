package com.stockmatch.corporate.incomestatement.service;

import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.common.infra.GenericAlphaVantageClient;
import com.stockmatch.corporate.incomestatement.dto.IncomeStatementDto;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeStatementService {

    private final GenericCacheService cacheService;
    private final GenericAlphaVantageClient apiClient;
    private final MemberService memberService;

    private static final String function = "income_statement";
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    public IncomeStatementDto getIncomeStatement(Long userId, String symbol){

        return cacheService.getOrLoad(
                function,
                symbol,
                IncomeStatementDto.class,
                CACHE_TTL,
                () -> {
                    String apiKey = memberService.getDecryptedApiKey(userId);

                    return apiClient.fetchData(
                            function,
                            symbol,
                            apiKey,
                            IncomeStatementDto.class
                    );
                }
        );

    }
}
