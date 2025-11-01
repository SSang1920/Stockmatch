package com.stockmatch.stock.infra.finnhub;

import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class FinnhubClient {

    private final RestTemplate restTemplate;

    @Value("${finnhub.base-url}")
    private String baseUrl;

    @Value("${finnhub.api-key}")
    private String apiKey;

    public FinnhubQuoteResponse getQuote(String symbol) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/quote")
                .queryParam("symbol", symbol)
                .queryParam("token", apiKey)
                .toUriString();

        return restTemplate.getForObject(url, FinnhubQuoteResponse.class);
    }
}
