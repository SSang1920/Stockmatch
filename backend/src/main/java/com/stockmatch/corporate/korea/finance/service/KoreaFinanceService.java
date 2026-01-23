package com.stockmatch.corporate.korea.finance.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.korea.common.domain.DartCorpCode;
import com.stockmatch.corporate.korea.common.infra.GenericDartClient;
import com.stockmatch.corporate.korea.common.domain.DartCorpCodeRepository;
import com.stockmatch.corporate.korea.finance.dto.DartFinancialRawResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KoreaFinanceService {

    private final GenericCacheService cacheService;
    private final GenericDartClient apiClient;
    private final DartCorpCodeRepository dartCorpCodeRepository;

    private static final String function = "korea_finance";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    /**
     * 요약된 재무 정보를 가져오는 메소드
     */
    public DartFinancialRawResponse getFinancialData(String symbol, String year, String reportCode) {
        String cacheKey = symbol + ":" + year + ":" + reportCode;

        return cacheService.getOrLoad(
                function,
                cacheKey,
                DartFinancialRawResponse.class,
                CACHE_TTL,
                () -> {
                    //티커로 고유 번호 조회
                    DartCorpCode dartCorpCode = dartCorpCodeRepository.findByTicker(symbol)
                            .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

                    //DART API 전용 파라미터
                    Map<String, String> params = Map.of(
                            "corp_code", dartCorpCode.getCorpCode(),
                            "bsns_year", year,
                            "reprt_code", reportCode,
                            "fs_div", "CFS"
                    );

                    log.info("Cache Miss! Fetching data from DART for : {}" , symbol);

                    return apiClient.fetchJsonData(
                            "/fnlttSinglAcntAll.json",
                            params,
                            DartFinancialRawResponse.class
                    );

                }
        );
    }

}
