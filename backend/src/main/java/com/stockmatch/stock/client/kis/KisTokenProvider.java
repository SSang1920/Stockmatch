package com.stockmatch.stock.client.kis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KisTokenProvider {

    private static final String KIS_TOKEN_KEY = "kis:access_token";
    private static final Duration KIS_TOKEN_TTL = Duration.ofHours(23);

    private final KisAuthClient kisAuthClient;
    private final StringRedisTemplate redisTemplate;

    public synchronized String getAccessToken() {
        // Redis에서 토큰 조회
        String token = redisTemplate.opsForValue().get(KIS_TOKEN_KEY);
        if (token != null && !token.isBlank()) {
            return token; // Redis에 있으면 바로 반환
        }

        // 새 토큰 발급
        var response = kisAuthClient.requestAccessToken();
        String newToken = response.getAccessToken();

        // 다시 23시간짜리 TTL을 부여하여 Redis에 저장
        redisTemplate.opsForValue().set(KIS_TOKEN_KEY, newToken, KIS_TOKEN_TTL);

        return newToken;
    }
}
