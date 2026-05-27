package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.stock.dto.StockTrendResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockRankService {

    private final DomesticTrendService domesticTrendService;
    private final OverseasTrendService overseasTrendService;

    // Redis
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY_MARKET_TREND = "market:trend";    // Redis Key

    /**
     * 서버 시작 시 캐시 예열
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Market Trend Cache...");
            updateMarketTrendCache();
        } catch (Exception e) {
            log.error("Failed to initialize Market Trend Cache. It will be retried by Scheduler.");
        }
    }

    /**
     * 트렌드 데이터 조회 (Redis 우선)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMarketTrends() {
        try {
            // Redis 조회
            Map<String, Object> cachedData =
                    (Map<String, Object>) redisTemplate.opsForValue().get(REDIS_KEY_MARKET_TREND);

            if (cachedData != null) {
                return cachedData;
            }
        } catch (Exception e) {
            log.error("Redis trend fetch failed", e);
        }

        // 캐시 없거나 실패 시 API 호출
        try {
            return updateMarketTrendCache();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch market trends", e);
            return new HashMap<>();
        }
    }

    /**
     * (스케쥴러) 10분마다 갱신
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public Map<String, Object> updateMarketTrendCache() {
        log.info("Updating Market Trend Cache...");

        // 결과 맵 생성
        Map<String, Object> result = new HashMap<>();

        try {
            // 국내 데이터 (KR)
            Map<String, List<StockTrendResponse>> krData = new HashMap<>();
            krData.put("mostActive", domesticTrendService.getMostActive());
            krData.put("gainers", domesticTrendService.getGainers());
            krData.put("losers", domesticTrendService.getLosers());
            result.put("KR", krData);

            // 해외 데이터 (US)
            Map<String, List<StockTrendResponse>> usData = new HashMap<>();
            usData.put("mostActive", overseasTrendService.getMostActive());
            usData.put("gainers", overseasTrendService.getGainers());
            usData.put("losers", overseasTrendService.getLosers());
            result.put("US", usData);

            // Redis 저장
            redisTemplate.opsForValue().set(REDIS_KEY_MARKET_TREND, result, 20, TimeUnit.MINUTES);
            log.info("Market Trend cached successfully.");

        } catch (Exception e) {
            log.error("Error collecting trend data, skipping cache update", e);
        }

        return result;
    }

    private List<StockTrendResponse> safeGet(Supplier<List<StockTrendResponse>> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("부분적인 데이터 조회 실패, 빈 리스트로 대체합니다.");
            return Collections.emptyList();
        }
    }
}
