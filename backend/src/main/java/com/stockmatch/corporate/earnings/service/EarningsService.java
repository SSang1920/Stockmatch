package com.stockmatch.corporate.earnings.service;

import com.stockmatch.corporate.earnings.cache.EarningsCacheService;
import com.stockmatch.corporate.earnings.client.ExternalEarningsClient;
import com.stockmatch.corporate.earnings.dto.EarningsDto;
import com.stockmatch.corporate.earnings.infra.AlphaVantageEarningsClient;
import com.stockmatch.corporate.overview.dto.CompanyOverviewDto;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsService {

    private final EarningsCacheService cacheService;
    private final ExternalEarningsClient earningsClient;
    private final MemberService memberService;

    public EarningsDto getEarnings(Long userId, String symbol) {

        return cacheService.getOrLoadEarnings(symbol, () -> {

            // 사용자 API 키를 가져오기
            String apiKey = memberService.getDecryptedApiKey(userId);
            // API 클라이언트를 호출
            return earningsClient.getEarnings(symbol, apiKey);
        });
    }
}
