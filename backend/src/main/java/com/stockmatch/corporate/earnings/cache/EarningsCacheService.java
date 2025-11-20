package com.stockmatch.corporate.earnings.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.corporate.earnings.dto.EarningsDto;
import com.stockmatch.corporate.overview.dto.CompanyOverviewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsCacheService {

    private static final long LOCK_WAIT_MS = 3000L;
    private static final long LOCK_SLEEP_MS = 50L;
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private String key(String symbol){
        return "corporate:earnings:" + symbol;
    }

    private String lockKey(String symbol){
        return "lock:corporate:earnings::" + symbol;
    }

    // JSON -> DTO
    private Optional<EarningsDto> parse(String json) {
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, EarningsDto.class));
        } catch (Exception e) {
            log.warn("EarningsCache parse fail. err={}", e.toString());
            return Optional.empty();
        }
    }

    // DTO -> JSON
    private String stringify(EarningsDto v) {
        try {
            return objectMapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            log.warn("EarningsCache write fail. err={}", e.toString());
            return null;
        }
    }

    // --- 캐시 조회/저장 로직 (헬퍼 메서드) ---
    public Optional<EarningsDto> getCached(String symbol) {
        String v = redisTemplate.opsForValue().get(key(symbol));
        return parse(v);
    }

    public void put(String symbol, EarningsDto response) {
        String json = stringify(response);
        if (json == null) return;
        redisTemplate.opsForValue().set(key(symbol), json, CACHE_TTL);
    }

    public EarningsDto getOrLoadEarnings(String symbol, Supplier<EarningsDto> loader){

        var cached = getCached(symbol);
        if (cached.isPresent()) return cached.get();

        final String lockKey = lockKey(symbol);
        final String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // 더블 체크
                var again = getCached(symbol);
                if (again.isPresent()) {
                    return again.get();
                }

                // 외부 API 로드
                var fresh = loader.get();
                if (fresh != null) {
                    put(symbol, fresh);
                }
                return fresh;
            } finally {
                // 락 해제
                String v = redisTemplate.opsForValue().get(lockKey);
                if (token.equals(v)) {
                    redisTemplate.delete(lockKey);
                }
            }
        } else {
            long deadline = System.currentTimeMillis() + LOCK_WAIT_MS;
            while (System.currentTimeMillis() < deadline) {
                var v = getCached(symbol);
                if (v.isPresent()) {
                    return v.get();
                }
                try {
                    Thread.sleep(LOCK_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // 타임아웃이면 직접 로드
            return loader.get();
        }

    }
}
