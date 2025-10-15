package com.stockmatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class StockmatchApplicationTests {

	@MockitoBean
	org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

	@Test
	void contextLoads() {
	}
}
