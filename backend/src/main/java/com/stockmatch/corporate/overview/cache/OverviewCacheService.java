package com.stockmatch.corporate.overview.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OverviewCacheService {

    private static final long LOCK_WAIT_MS = 3000L;
    private static final long LOCK_SLEEP_MS = 50L;
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // --- 키 생성 규칙 ---
    private String key(String symbol) {
        return "corporate:overview:" + symbol;
    }

    private String lockKey(String symbol) {
        return "lock:corporate:overview::" + symbol;
    }

    // --- JSON 변환 헬퍼 ---
    private Optional<CompanyOverviewDto> parse(String json) {
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, CompanyOverviewDto.class));
        } catch (Exception e) {
            log.warn("OverviewCache parse fail. err={}", e.toString());
            return Optional.empty();
        }
    }

    private String stringify(CompanyOverviewDto v) {
        try {
            return objectMapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            log.warn("OverviewCache write fail. err={}", e.toString());
            return null;
        }
    }

    // --- 캐시 조회/저장 로직 (헬퍼 메서드) ---
    public Optional<CompanyOverviewDto> getCached(String symbol) {
        String v = redisTemplate.opsForValue().get(key(symbol));
        return parse(v);
    }

    public void put(String symbol, CompanyOverviewDto response) {
        String json = stringify(response);
        if (json == null) return;
        redisTemplate.opsForValue().set(key(symbol), json, CACHE_TTL);
    }


    public CompanyOverviewDto getOrLoadOverview(String symbol, Supplier<CompanyOverviewDto> loader){

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
