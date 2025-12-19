package com.stockmatch.corporate.global.news.service;

import com.stockmatch.corporate.common.cache.GenericCacheService;
import com.stockmatch.corporate.global.common.infra.GenericAlphaVantageClient;
import com.stockmatch.corporate.global.news.dto.NewsSentimentDto;
import com.stockmatch.user.member.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final GenericAlphaVantageClient alphaVantageClient;
    private final GenericCacheService cacheService;

    public NewsSentimentDto getNewsSentiment(String tickers, User user){
        String apiKey = user.getAlphaVantageKey().getKeyCipher();

        String cacheKey = (tickers != null && !tickers.isBlank()) ? tickers : "all";

        return cacheService.getOrLoad(
                "news_sentiment",
                cacheKey,
                NewsSentimentDto.class,
                Duration.ofHours(1),
                () -> {
                    Map<String, String> params = new HashMap<>();

                    if(tickers != null && !tickers.isBlank()) {
                        params.put("tickers", tickers);
                    }

                    return alphaVantageClient.fetchDataWithParams(
                            "NEWS_SENTIMENT",
                            params,
                            apiKey,
                            NewsSentimentDto.class
                    );
                }
        );
    }
}
