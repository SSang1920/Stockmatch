package com.stockmatch.corporate.earnings.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.corporate.earnings.client.ExternalEarningsClient;
import com.stockmatch.corporate.earnings.dto.EarningsDto;
import com.stockmatch.corporate.overview.dto.CompanyOverviewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component("alphaVantageEarningsClient")
@RequiredArgsConstructor
public class AlphaVantageEarningsClient implements ExternalEarningsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.base-url}")
    private String baseUrl;

    @Override
    public EarningsDto getEarnings(String symbol, String apiKey){
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/query")
                .queryParam("function", "EARNINGS")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .build(true).toUri();

        log.info("Requesting Company Earnings from Alpha Vantage for symbol: {}", symbol);

        Map<String, Object> rawResponse = restTemplate.getForObject(uri, Map.class);

        log.info("Raw API Response from Alpha Vantage: {}", rawResponse);

        if (rawResponse == null || rawResponse.isEmpty()) {

            return null;
        }

        return objectMapper.convertValue(rawResponse, EarningsDto.class);


    }
}
