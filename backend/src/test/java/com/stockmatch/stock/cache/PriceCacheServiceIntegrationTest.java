package com.stockmatch.stock.cache;

import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class PriceCacheServiceIntegrationTest {

    @Container
    static GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("price.cache.ttl-seconds", () -> "3");
    }

    @Autowired PriceCacheService priceCache;
    @Autowired StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void flushAll() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private StockPriceResponse sample(String ticker, double price) {
        return StockPriceResponse.builder()
                .region(Region.US)
                .ticker(ticker)
                .name(ticker)
                .currentPrice(price)
                .prevClose(price - 1)
                .openPrice(price - 0.5)
                .highPrice(price + 1)
                .lowPrice(price - 2)
                .changeRate(0.01)
                .build();
    }

    @Test
    @DisplayName("단건 getOrLoad: 미스->fetch 저장, 히트->캐시 반환")
    void singleGetOrLoad() throws Exception {
        var fresh = sample("AAPL", 100.0);
        var res1 = priceCache.getOrLoad("US", "AAPL", () -> fresh);
        assertThat(res1.getCurrentPrice()).isEqualTo(100.0);

        // 두 번째는 캐시 히트로 바로 반환
        var res2 = priceCache.getOrLoad("US", "AAPL", Assertions::fail);
        assertThat(res2.getCurrentPrice()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("다건 getOrLoadBulk: 캐시 히트 + 캐시 미스 처리")
    void bulkGetOrLoad() {
        // 사전 캐시
        priceCache.put("US", "AAPL", sample("AAPL", 100.0));

        var symbols = List.of("AAPL", "MSFT", "TSLA");
        var result = priceCache.getOrLoadBulk(
                "US",
                symbols,
                miss -> Map.of(
                        "MSFT", sample("MSFT", 200.0),
                        "TSLA", sample("TSLA", 300.0)
                )
        );

        assertThat(result).hasSize(3);
        assertThat(result.get("AAPL").getCurrentPrice()).isEqualTo(100.0);
        assertThat(result.get("MSFT").getCurrentPrice()).isEqualTo(200.0);
        assertThat(result.get("TSLA").getCurrentPrice()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("TTL/evict 동작 확인")
    void ttlAndEvict() throws InterruptedException {
        priceCache.put("US", "NVDA", sample("NVDA", 500.0));

        long ttl = priceCache.ttlSeconds("US", "NVDA");
        assertThat(ttl).isGreaterThan(0);

        priceCache.evict("US", "NVDA");
        long ttlAfterEvict = priceCache.ttlSeconds("US", "NVDA");
        assertThat(ttlAfterEvict).isEqualTo(-2);
    }
}
