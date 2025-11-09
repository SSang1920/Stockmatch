package com.stockmatch.stock.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCacheService {

    // 락 대기 최대 시간
    private static final long LOCK_WAIT_MS = 3000L;
    private static final long LOCK_SLEEP_MS = 50L;
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    @Value("${price.cache.ttl-seconds}")
    private int ttlSeconds;

    /**
     * Redis 키 생성 규칙
     */
    private String key(String region, String symbol) {
        return "price:" + region + ":" + symbol;
    }

    private String lockKey(String region, String symbol) {
        return "lock:price:" + region + ":" + symbol;
    }

    /**
     * TTL 지터 추가 -> 동시 만료로 인한 외부 API 부하 방지
     */
    private Duration ttlWithJitter() {
        int base = Math.max(1, ttlSeconds);
        double factor = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
        long sec = Math.max(1, Math.round(base * factor));

        return Duration.ofSeconds(sec);
    }

    /**
     * JSON -> StockPriceResponse 변환 (실패시 empty)
     */
    private Optional<StockPriceResponse> parse(String json) {
        if (json == null) return Optional.empty();

        try {
            return Optional.of(om.readValue(json, StockPriceResponse.class));
        } catch (Exception e) {
            log.warn("PriceCache parse fail. err={}", e.toString());
            return Optional.empty();
        }
    }

    /**
     * StockPriceResponse -> JSON 변환 (실패시 null)
     */
    private String stringify(StockPriceResponse v) {
        try {
            return om.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            log.warn("PriceCache write fail. err={}", e.toString());
            return null;
        }
    }

    /**
     * 단건 조회: 캐시 우선 조회 -> 캐시 미스면 fetcher(외무 API 호출)로 로드 후 캐시에 저장
     */
    public StockPriceResponse getOrLoad(String region, String symbol, Supplier<StockPriceResponse> fetcher) {
        // 캐시 먼저 확인
        var cached = getCached(region, symbol);
        if (cached.isPresent()) return cached.get();

        final String lockKey = lockKey(region, symbol);
        final String token = UUID.randomUUID().toString();

        // 락 시도
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);
        if (Boolean.TRUE.equals(acquired)) {
            try {
                // 더블 체크
                var again = getCached(region, symbol);
                if (again.isPresent()) return again.get();

                // 외부 API 로드
                var fresh = fetcher.get();
                if (fresh != null) put(region, symbol, fresh);

                return fresh;
            } finally {
                // 락 해제
                String v = redis.opsForValue().get(lockKey);
                if (token.equals(v)) {
                    redis.delete(lockKey);
                }
            }
        } else {
            // 캐시가 채워지길 대기
            long deadline = System.currentTimeMillis() + LOCK_WAIT_MS;
            while (System.currentTimeMillis() < deadline) {
                var v = getCached(region, symbol);
                if (v.isPresent()) return v.get();
                try {
                    Thread.sleep(LOCK_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 타임아웃이면 직접 로드(과한 지연 방지)
            var fresh = fetcher.get();
            if (fresh != null) put(region, symbol, fresh);
            return fresh;
        }
    }

    /**
     * 다건 캐시 조회: 캐시 우선 조회 -> 캐시 미스면 fetcher(외무 API 호출)로 로드 후 캐시에 저장
     */
    public Map<String, StockPriceResponse> getOrLoadBulk(
            String region,
            List<String> symbols,
            Function<List<String>, Map<String, StockPriceResponse>> fetcher
    ) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, StockPriceResponse> out = new LinkedHashMap<>();
        for (String s : symbols) {
            StockPriceResponse v = this.getOrLoad(region, s, () -> {
                // 단건 호출일 때 한 종목만 요청하도록 감싸기
                Map<String, StockPriceResponse> one = fetcher.apply(List.of(s));
                return one != null ? one.get(s) : null;
            });

            if (v != null) out.put(s, v);
        }

        return out;
    }

    /**
     * 캐시 단건 조회
     */
    public Optional<StockPriceResponse> getCached(String region, String symbol) {
        // 문자열 값 조회(존재하지 않으면 null)
        String v = redis.opsForValue().get(key(region, symbol));
        return parse(v);
    }

    /**
     * 캐시에 시세 저장(만료시간 TTL 포함)
     */
    public void put(String region, String symbol, StockPriceResponse price) {
        String json = stringify(price);

        if (json == null) return;
        redis.opsForValue().set(key(region, symbol), json, ttlWithJitter());
    }


    /**
     * 캐시 강제 삭제
     */
    public void evict(String region, String symbol) {
        redis.delete(key(region, symbol));
    }

    /**
     * 현재 키의 남은 TTL 조회
     * 반환값) -2: 키 존재 X, -1: 무기한, >=0: 남은 초
     */
    public long ttlSeconds(String region, String symbol) {
        Long ttl = redis.getExpire(key(region, symbol), TimeUnit.SECONDS);
        return ttl == null ? -2 : ttl;
    }
}
