package com.stockmatch.stock.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.stock.client.kis.dto.MinutePriceItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartCacheService {

    private static final long LOCK_WAIT_MS = 5000L;
    private static final long LOCK_SLEEP_MS = 100L;
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    private static final int DEFAULT_TTL_SECONDS = 60;

    /**
     * Redis 키 생성 규칙
     */
    private String key(String ticker) {
        return "chart:minute:" + ticker;
    }

    private String lockKey(String ticker) {
        return "lock:chart:" + ticker;
    }

    /**
     * TTL 지터 추가
     */
    private Duration ttlWithJitter() {
        int base = DEFAULT_TTL_SECONDS;
        double factor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
        long sec = Math.max(1, Math.round(base * factor));
        return Duration.ofSeconds(sec);
    }

    /**
     * JSON -> List<MinutePriceItem> 변환
     */
    private Optional<List<MinutePriceItem>> parse(String json) {
        if (json == null) return Optional.empty();
        try {
            return Optional.of(om.readValue(json, new TypeReference<List<MinutePriceItem>>() {
            }));
        } catch (Exception e) {
            log.warn("ChartCache parse fail. err={}", e.toString());
            return Optional.empty();
        }
    }

    /**
     * List<MinutePriceItem> -> Json 변환
     */
    private String stringify(List<MinutePriceItem> v) {
        try {
            return om.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            log.warn("ChartCache write fail. err={}", e.toString());
            return null;
        }
    }

    /**
     * 캐시 조회 (없으면 fetcher 실행)
     */
    public List<MinutePriceItem> getOrLoad(String ticker, Supplier<List<MinutePriceItem>> fetcher) {
        // 캐시 조회
        Optional<List<MinutePriceItem>> cached = getCached(ticker);
        if (cached.isPresent()) return cached.get();

        final String lockKey = lockKey(ticker);
        final String token = UUID.randomUUID().toString();

        // 락 시도
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // 더블 체크
                Optional<List<MinutePriceItem>> again = getCached(ticker);
                if (again.isPresent()) return again.get();

                // 외부 API 호출
                List<MinutePriceItem> fresh = fetcher.get();

                // 데이터가 있으면 캐시에 저장
                if (fresh != null && !fresh.isEmpty()) {
                    put(ticker, fresh);
                } else {
                    fresh = Collections.emptyList();
                }

                return fresh;
            } finally {
                // 락 해제
                String v = redis.opsForValue().get(lockKey);
                if (token.equals(v)) {
                    redis.delete(lockKey);
                }
            }
        } else {
            // 락 획득 실패 시 대기
            long deadline = System.currentTimeMillis() + LOCK_WAIT_MS;
            while (System.currentTimeMillis() < deadline) {
                Optional<List<MinutePriceItem>> v = getCached(ticker);
                if (v.isPresent()) return v.get();
                try {
                    Thread.sleep(LOCK_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 대기 시간 초과 시 직접 로드
            List<MinutePriceItem> fresh = fetcher.get();
            if (fresh != null && !fresh.isEmpty()) put(ticker, fresh);
            return fresh != null ? fresh : Collections.emptyList();
        }
    }

    public Optional<List<MinutePriceItem>> getCached(String ticker) {
        String v = redis.opsForValue().get(key(ticker));
        return parse(v);
    }

    public void put(String ticker, List<MinutePriceItem> data) {
        String json = stringify(data);
        if (json == null) return;
        redis.opsForValue().set(key(ticker), json, ttlWithJitter());
    }
}
