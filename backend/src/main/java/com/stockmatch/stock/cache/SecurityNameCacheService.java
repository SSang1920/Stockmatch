package com.stockmatch.stock.cache;

import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SecurityNameCacheService {

    private final StringRedisTemplate redis;
    private final SecurityRepository securityRepository;

    @Value("${namecache.prefix}")
    private String prefix;

    @Value("${namecache.ttl-seconds}")
    private long ttlSeconds;

    @Value("${namecache.kr-namespace}")
    private String krNamespace;

    private String keyKR(String tikcer6) {
        return prefix + ":" + krNamespace + ":" + tikcer6;
    }

    /**
     * 티커 정규화
     */
    public String normizeTicker(String ticker) {
        if (ticker == null) return null;

        String t = ticker.trim();
        if (t.matches("\\d")) return String.format("%06d", Integer.parseInt(t));
        return t;
    }

    /**
     * KR 종목명 조회: Redis -> DB -> null
     */
    public String getKrName(String rawTicker) {
        String ticker = normizeTicker(rawTicker);
        if (ticker == null || ticker.isBlank()) return null;

        // Redis 조회
        try {
            String cache = redis.opsForValue().get(keyKR(ticker));
            if (cache != null && !cache.isBlank()) return cache;
        } catch (Exception e) {}

        // DB 조회
        String dbName = securityRepository
                .findByTickerAndMarket(ticker, Market.KR)
                .map(s -> s.getName() == null ? null :s.getName().trim())
                .orElse(null);

        if (dbName != null && !dbName.isBlank()) {
            putKr(ticker, dbName, ttlSeconds);
            return dbName;
        }
        return null;
    }

    /**
     * KR 종목명 캐시 저장
     */
    public void putKr(String rawTicker, String name, long ttlSec) {
        String ticker = normizeTicker(rawTicker);
        if (ticker == null || ticker.isBlank() || name == null | name.isBlank()) return;
        try {
            redis.opsForValue().set(keyKR(ticker), name, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {}
    }

    /**
     * 캐시 무효화
     */
    public void evictKr(String rawTicker) {
        String ticker = normizeTicker(rawTicker);
        if (ticker == null || ticker.isBlank()) return;
        try {
            redis.delete(keyKR(ticker));
        } catch (Exception e) {}
    }
}
