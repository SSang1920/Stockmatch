package com.stockmatch.exchangeRate.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateCacheService {

    private static final long LOCK_WAIT_MS = 3000L;
    private static final long LOCK_SLEEP_MS = 50L;
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final Duration CURRENT_RATE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // --- 키 생성 규칙 ---
    private String key(LocalDate date, FromCurrency from, ToCurrency to) {
        return "exchangeRate:" + date + ":" + from.name() + ":" + to.name();
    }

    private String lockKey(LocalDate date, FromCurrency from, ToCurrency to) {
        return "lock:exchangeRate:" + date + ":" + from.name() + ":" + to.name();
    }

    private String currentRateKey(FromCurrency from, ToCurrency to) {
        return "exchangeRate:current:" + from.name() + ":" + to.name();
    }

    // --- JSON 변환 헬퍼 ---
    private Optional<ExchangeRate> parse(String json) {
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ExchangeRate.class));
        } catch (Exception e) {
            log.warn("ExchangeRateCache parse fail. err={}", e.toString());
            return Optional.empty();
        }
    }

    private String stringify(ExchangeRate v) {
        try {
            return objectMapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            log.warn("ExchangeRateCache write fail. err={}", e.toString());
            return null;
        }
    }

    /**
     * 실시간 환율 관리
     */
    // 현재 환율 저장
    public void saveCurrentRate(FromCurrency from, ToCurrency to, BigDecimal rate) {
        if (rate == null) return;
        String key = currentRateKey(from, to);
        redisTemplate.opsForValue().set(key, rate.toString(), CURRENT_RATE_TTL);
    }

    // 현재 환율 조회
    public BigDecimal getCurrentRate(FromCurrency from, ToCurrency to) {
        String key = currentRateKey(from, to);
        String val = redisTemplate.opsForValue().get(key);

        if (val != null) {
            try {
                return new BigDecimal(val);
            } catch (NumberFormatException e) {
                log.error("Invalid exchange rate in cache: {}", val);
            }
        }
        return null;
    }

    // --- 캐시 조회/저장 로직 ---
    public Optional<ExchangeRate> getCached(LocalDate date, FromCurrency from, ToCurrency to) {
        String v = redisTemplate.opsForValue().get(key(date, from, to));

        return parse(v);
    }

    public void put(LocalDate date, FromCurrency from, ToCurrency to, ExchangeRate rate) {
        String json = stringify(rate);

        if (json == null) {
            return;
        }
        redisTemplate.opsForValue().set(key(date, from, to), json, CACHE_TTL);
    }

    public ExchangeRate getOrLoadExchangeRate(LocalDate date, FromCurrency from, ToCurrency to, Supplier<ExchangeRate> loader){

        //캐시 먼저 조회
        var cached = getCached(date, from, to);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 락 시도
        final String lockKey = lockKey(date, from, to);
        final String token = UUID.randomUUID().toString();
        //lockKey 없을시 생성하고 true 반환, lockKey없을시 false 반환
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            // 락 획득 성공
            try {
                // 더블 체크
                var again = getCached(date, from, to);
                if (again.isPresent()) {
                    return again.get();
                }

                // 외부 API 로드
                var fresh = loader.get();
                if (fresh != null) {
                    put(date, from, to, fresh);
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
            // 락 획득 실패
            long deadline = System.currentTimeMillis() + LOCK_WAIT_MS;

            while (System.currentTimeMillis() < deadline) {
                var v = getCached(date, from, to);
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
