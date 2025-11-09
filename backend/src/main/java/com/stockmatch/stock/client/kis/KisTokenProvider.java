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

    // 메모리 캐시에 들고 있을 값
    private String cachedToken;

    public synchronized String getAccessToken() {
        // 인스턴스 안에 있으면 먼저 가져오기
        if (cachedToken != null && !cachedToken.isBlank()) {
            return cachedToken;
        }

        // Redis에 있는지 확인 후 가져오기
        String token = redisTemplate.opsForValue().get(KIS_TOKEN_KEY);
        if (token != null && !token.isBlank()) {
            this.cachedToken = token;
            return token;
        }

        // 없으면 KIS에서 새로 발급
        var response = kisAuthClient.requestAccessToken();
        String newToken = response.getAccessToken();

        // Redis에 저장 + TTL
        redisTemplate.opsForValue().set(KIS_TOKEN_KEY, newToken, KIS_TOKEN_TTL);

        // 메모리에 저장
        this.cachedToken = newToken;

        return newToken;
    }
}
