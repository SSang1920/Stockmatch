package com.stockmatch.corporate.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.corporate.common.dto.CacheableData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericCacheService {

    private static final long LOCK_WAIT_MS = 3000L;
    private static final long LOCK_SLEEP_MS = 50L;
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    /**
     * 캐시에서 조회, 없으면 API 호출하는 범용 메소드
     *
     * @param function   API 기능 (예: "overview", "earnings")
     * @param symbol    캐시 키를 완성하는 접미사 (예: "AAPL")
     * @param dto         반환받 DTO 클래스 (예: CompanyOverviewDto.class)
     * @param ttl          이 캐시의 유효 기간 (Time To Live)
     * @param loader       데이터가 없을 때 실행할 실제 데이터 로딩 작업
     * @param <T>          데이터의 타입
     * @return             캐시, 호출된 데이터 객체
     */
    public <T> T getOrLoad(
            String function,
            String symbol,
            Class<T> dto,
            Duration ttl,
            Supplier<T> loader
    ) {
        String cacheKey = "corporate:" + function + ":" + symbol;
        String lockKey = "lock:corporate:" + function + ":" + symbol;

        // 캐시 확인
        Optional<T> cached = getCached(cacheKey, dto);
        if (cached.isPresent()) {
            log.info("Cache hit for key: {}", cacheKey);
            return cached.get();
        }

        //락
        log.info("Cache miss for key: {}. Attempting to acquire lock.", cacheKey);
        final String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);

        if (Boolean.TRUE.equals(acquired)) {

            log.debug("Lock acquired for key: {}", cacheKey);
            try {
                // 더블 체크
                var again = getCached(cacheKey, dto);
                if (again.isPresent()) return again.get();

                // 외부 API 호출
                var fresh = loader.get();
                if (fresh instanceof CacheableData cacheableData) {
                    if (cacheableData.isValidForCaching()) {
                        put(cacheKey, fresh, ttl);
                        log.info("유효한 데이터 캐시 저장 완료. key: {}", cacheKey);
                    } else {
                        log.warn("캐시할 수 없는 유효하지 않은 데이터 수신. key: {}", cacheKey);
                    }
                } else if (fresh != null) {
                    put(cacheKey, fresh, ttl);
                    log.info("캐시 저장 완료 (유효성 검사 없음). key: {}", cacheKey);
                }
                return fresh;
            } finally {
                // 락 해제
                String v = redisTemplate.opsForValue().get(lockKey);
                if (token.equals(v)) {
                    redisTemplate.delete(lockKey);
                    log.debug("Lock released for key: {}", cacheKey);
                }
            }
        } else {
            //  락 획득 실패
            log.debug("Failed to acquire lock for key: {}. Waiting...", cacheKey);
            long deadline = System.currentTimeMillis() + LOCK_WAIT_MS;
            while (System.currentTimeMillis() < deadline) {
                var v = getCached(cacheKey, dto);
                if (v.isPresent()) return v.get();
                try {
                    Thread.sleep(LOCK_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // 타임아웃이면 직접 로드
            log.warn("Lock wait timeout. Loading directly. key={}", cacheKey);
            return loader.get();
        }
    }

    // --- 범용 헬퍼 메서드 ---
    private <T> Optional<T> getCached(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, type)); // JSON -> DTO
        } catch (Exception e) {
            log.warn("Cache parse fail. key={}, err={}", key, e.toString());
            return Optional.empty();
        }
    }

    private void put(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value); //DTO -> JSON
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Cache write fail. key={}, err={}", key, e.toString());
        }
    }
}
