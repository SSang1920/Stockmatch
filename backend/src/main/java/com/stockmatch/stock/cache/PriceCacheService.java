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

        // 캐시 미스면 외부 API 호출하여 가져오기
        var fresh = fetcher.get();

        // null 방어
        if (fresh != null) put(region, symbol, fresh);

        return fresh;
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

        // 캐시 일괄 조회
        List<String> keys = symbols.stream().map(s -> key(region, s)).toList();
        List<String> rawList = redis.opsForValue().multiGet(keys);
        if (rawList == null) rawList = Collections.emptyList();

        Map<String, StockPriceResponse> hitMap = new LinkedHashMap<>();
        List<String> misses = new ArrayList<>();

        for (int i = 0; i < symbols.size(); i++) {
            String sym = symbols.get(i);
            String raw = (i < rawList.size()) ? rawList.get(i) : null;
            var parsed = parse(raw);

            if (parsed.isPresent()) {
                hitMap.put(sym, parsed.get());
            } else {
                misses.add(sym);
            }
        }

        // 캐시 미스시 외부 호출
        if (!misses.isEmpty()) {
            Map<String, StockPriceResponse> fetched = fetcher.apply(misses);
            if (fetched != null && !fetched.isEmpty()) {
                // 캐시 채우기
                fetched.forEach((sym, price) -> {
                    if (price != null) put(region, sym, price);
                });

                // 결과 합치기(입력 순서 유지)
                for (String sym : misses) {
                    var v = fetched.get(sym);
                    if (v != null) hitMap.put(sym, v);
                }
            }
        }

        return hitMap;
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
