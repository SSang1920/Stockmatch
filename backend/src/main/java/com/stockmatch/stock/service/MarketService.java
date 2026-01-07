package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.kis.KisKorStockClient;
import com.stockmatch.stock.client.kis.KisUsStockClient;
import com.stockmatch.stock.dto.MarketOverviewResponse;
import com.stockmatch.stock.dto.StockPriceResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final KisKorStockClient kisKorStockClient;
    private final KisUsStockClient kisUsStockClient;

    // Redis
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY_MARKET_OVERVIEW = "market:overview"; // Redis Key

    // KIS API 지수 코드
    private static final String CODE_KOSPI = "0001";        // 코스피
    private static final String CODE_NASDAQ = "COMP";       // 나스닥
    private static final String CODE_SP500 = "SPX";         // S&P 500
    private static final String CODE_EXCHANGE = "FX@KRW";   // 원달러 환율

    /**
     * 서버 시작 시 즉시 실행
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Market Overview Data...");
            fetchAndCacheMarketData();
        } catch (Exception e) {
            log.error("Failed to initialize Market Overview Data. It will be retried by Scheduler.", e);
        }

    }

    /**
     * 마켓 오버뷰 조회 (Redis 우선)
     * 1차: Redis 조회
     * 2차: 실패/없음 시 API 호출 후 갱신
     */
    public MarketOverviewResponse getGlobalMarketOverview() {
        try {
            // Redis에서 데이터 조회 시도
            MarketOverviewResponse cacheData = (MarketOverviewResponse) redisTemplate.opsForValue().get(REDIS_KEY_MARKET_OVERVIEW);
            if (cacheData != null) {
                return cacheData;
            }
        } catch (Exception e) {
            log.error("Redis operation failed: {}", e.getMessage());
        }

        // 캐시 없거나 오류 시 API 호출
        return fetchAndCacheMarketData();
    }

    /**
     * (스케쥴러) 정각기준 5분마다 API 호출하여 Redis 갱신
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void updateMarketOverviewCache() {
        try {
            log.info("Executing Scheduled Task: Update Market Overview Cache");
            fetchAndCacheMarketData();
        } catch (Exception e) {
            log.error("Scheduled Market Update Failed", e);
        }

    }

    /**
     * API 호출 및 Redis 저장 로직
     */
    private MarketOverviewResponse fetchAndCacheMarketData() {
        try {
            // 코스피 조회
            StockPriceResponse kospi = kisKorStockClient.getKrIndexPrice(CODE_KOSPI);

            // 나스닥, S&P 500 조회
            StockPriceResponse nasdaq = kisUsStockClient.getUsIndexPrice(CODE_NASDAQ, "NASDAQ");
            StockPriceResponse sp500 = kisUsStockClient.getUsIndexPrice(CODE_SP500, "S&P 500");

            // 환율 조회
            StockPriceResponse usdRate = kisUsStockClient.getUsIndexPrice(CODE_EXCHANGE, "USD/KRW");

            // 현재 시간 포맷
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM.dd HH:mm"));

            MarketOverviewResponse response = MarketOverviewResponse.builder()
                    .kospi(mapToIndex(kospi, "KOSPI"))
                    .nasdaq(mapToIndex(nasdaq, "NASDAQ"))
                    .sp500(mapToIndex(sp500, "S&P 500"))
                    .usdKrw(MarketOverviewResponse.ExchangeRateInfo.builder()
                            .name(usdRate.getName())
                            .rate(usdRate.getCurrentPrice())
                            .change(usdRate.getChangeAmount())
                            .changeRate(usdRate.getChangeRate())
                            .build())
                    .lastUpdateTime(now)
                    .build();

            // Redis 저장
            try {
                redisTemplate.opsForValue().set(REDIS_KEY_MARKET_OVERVIEW, response, 10, TimeUnit.MINUTES);
                log.info("Market Overview cached successfully.");
            } catch (Exception e) {
                log.error("Failed to cache market data: {}", e.getMessage());
            }

            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during market data fetch", e);
            throw new BusinessException(ErrorCode.MARKET_DATA_FETCH_ERROR);
        }
    }


    /**
     * StockPriceResponse -> MarketOverviewResponse.IndexInfo 변환 헬퍼 메서드
     */
    private MarketOverviewResponse.IndexInfo mapToIndex(StockPriceResponse src, String name) {
        if (src == null) {
            // 데이터가 없을 경우 이름만 담아서 빈 객체 반환
            return MarketOverviewResponse.IndexInfo.builder()
                    .name(name)
                    .price(BigDecimal.ZERO)
                    .change(BigDecimal.ZERO)
                    .changeRate(BigDecimal.ZERO)
                    .build();
        }

        return MarketOverviewResponse.IndexInfo.builder()
                .name(name)
                .price(src.getCurrentPrice())       // 현재가
                .change(src.getChangeAmount())      // 전일비
                .changeRate(src.getChangeRate())    // 등락률
                .build();
    }
}
