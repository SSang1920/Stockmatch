package com.stockmatch.corporate.analysis.guard;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AiRequestGuard {

    private final RedisTemplate<String,String> redisTemplate;

    private static final int DAILY_LIMIT = 20;
    private static final int RATE_LIMIT_SECONDS = 30;

    public void checkAvailabilityOrThrow(Long userId) {
        String today = LocalDate.now().toString();
        String quotaKey = "ai:quota:" + userId + ":" + today;
        String rateKey = "ai:rate:" + userId;

        // 검색 딜레이 체크
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // 일일 제한 체크
        String currentCount = redisTemplate.opsForValue().get(quotaKey);
        if (currentCount != null && Integer.parseInt(currentCount) >= DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.DAILY_QUOTA_EXCEEDED);
        }
    }

    public void incrementCount(Long userId) {
        String today = LocalDate.now().toString();
        String quotaKey = "ai:quota:" + userId + ":" + today;
        String rateKey = "ai:rate:" + userId;

        // 쿼터 카운트 증가 및 만료시간 24시간 설정
        redisTemplate.opsForValue().increment(quotaKey);
        redisTemplate.expire(quotaKey, Duration.ofDays(1));

        // 검색딜레이 키 생성 (설정된 초 동안 유지)
        redisTemplate.opsForValue().set(rateKey, "blocked", Duration.ofSeconds(RATE_LIMIT_SECONDS));
    }
}
