package com.stockmatch.stock.service;

import com.stockmatch.stock.client.kis.KisRankClient;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.StockTrendResponse;
import com.stockmatch.stock.repository.SecurityRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockRankService {

    private final KisRankClient kisRankClient;
    private final SecurityRepository securityRepository;

    // Redis
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY_MARKET_TREND = "market:trend";    // Redis Key

    /**
     * 서버 시작 시 캐시 예열
     */
    @PostConstruct
    public void init() {
        log.info("Initializing Market Trend Data...");
        updateMarketTrendCache();
    }

    /**
     * 트렌드 데이터 조회 (Redis 우선)
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<StockTrendResponse>> getMarketTrends() {
        try {
            // Redis 조회
            Map<String, List<StockTrendResponse>> cachedData =
                    (Map<String, List<StockTrendResponse>>) redisTemplate.opsForValue().get(REDIS_KEY_MARKET_TREND);

            if (cachedData != null) {
                return cachedData;
            }
        } catch (Exception e) {
            log.error("Redis trend fetch failed", e);
        }

        // 캐시 없으면 갱신 후 반환
        return updateMarketTrendCache();
    }

    /**
     * (스케쥴러) 10분마다 갱신
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public Map<String, List<StockTrendResponse>> updateMarketTrendCache() {
        log.info("Updating Market Trend Cache...");

        // KIS API 호출
        List<KisRankClient.KisVolumeItem> rawVolumeData = kisRankClient.getDomesticVolumeRank();

        // DB 병합 - 상위 10개 추출
        List<StockTrendResponse> mostActive = mergeWithDbData(rawVolumeData, 10);

        // 나중에 추가할 급등/급락 로직 (일단 가짜 데이터)
        // List<StockTrendResponse> gainers = getGainers();
        List<StockTrendResponse> gainers = Collections.emptyList();
        List<StockTrendResponse> losers = Collections.emptyList();

        // 결과 맵 생성
        Map<String, List<StockTrendResponse>> result = new HashMap<>();
        result.put("mostActive", mostActive);
        result.put("gainers", gainers);
        result.put("losers", losers);

        // Redis 저장
        try {
            redisTemplate.opsForValue().set(REDIS_KEY_MARKET_TREND, result, 20, TimeUnit.MINUTES);
            log.info("Market Trend cached successfully.");
        } catch (Exception e) {
            log.error("Failed to cache trend data", e);
        }

        return result;
    }

    /**
     * API 데이터 + DB 데이터 병합 로직
     */
    private List<StockTrendResponse> mergeWithDbData(List<KisRankClient.KisVolumeItem> apiItems, int limit) {
        if (apiItems == null || apiItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 상위 N개 티커 추출
        List<KisRankClient.KisVolumeItem> targetItems = apiItems.stream()
                .limit(limit)
                .toList();

        List<String> tickers = targetItems.stream()
                .map(KisRankClient.KisVolumeItem::getMkscShrnIscd)
                .toList();

        // DB Bulk 조회
        List<Security> securities = securityRepository.findByTickerIn(tickers);
        Map<String, Security> securityMap = securities.stream()
                .collect(Collectors.toMap(Security::getTicker, Function.identity()));

        // 변환 및 합치기
        return targetItems.stream()
                .map(item -> {
                    String ticker = item.getMkscShrnIscd();
                    Security dbSecurity = securityMap.get(ticker);

                    String finalName = (dbSecurity != null) ? dbSecurity.getName() : item.getHtsKorIsnm();
                    String market = (dbSecurity != null) ? dbSecurity.getMarket().name() : "KR";

                    return mapToDto(item, finalName, market);
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    /**
     * DTO 변환 헬퍼
     */
    private StockTrendResponse mapToDto(KisRankClient.KisVolumeItem item, String name, String market) {
        try {
            long currentPrice = Long.parseLong(item.getStckPrpr().replace(",", ""));
            long changeValue = Long.parseLong(item.getPrdyVrss().replace(",", ""));
            double changeRate = Double.parseDouble(item.getPrdyCtrt().replace(",", ""));

            String priceStr = String.format("%,d", currentPrice);
            String sign = changeValue > 0 ? "+" : (changeValue < 0 ? "-" : "");
            String changeStr = sign + " " + String.format("%,d", Math.abs(changeValue));
            String rateStr = (changeRate > 0 ? "+" : "") + String.format("%.2f%%", changeRate);

            return new StockTrendResponse(
                    item.getMkscShrnIscd(),
                    name,
                    priceStr,
                    changeStr,
                    rateStr,
                    market
            );
        } catch (Exception e) {
            log.warn("Error parsing rank item: {}", item.getMkscShrnIscd());
            return null;
        }
    }
}
