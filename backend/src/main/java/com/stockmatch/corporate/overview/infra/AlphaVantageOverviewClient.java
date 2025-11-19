package com.stockmatch.corporate.overview.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.corporate.overview.client.ExternalOverviewClient;
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
@Component("alphaVantageOverviewClient")
@RequiredArgsConstructor
public class AlphaVantageOverviewClient implements ExternalOverviewClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.base-url}")
    private String baseUrl;

   @Override
   public CompanyOverviewDto getCompanyOverview(String symbol, String apiKey){

       URI uri = UriComponentsBuilder
               .fromUriString(baseUrl)
               .path("/query")
               .queryParam("function", "OVERVIEW")
               .queryParam("symbol", symbol)
               .queryParam("apikey", apiKey)
               .build(true).toUri();

       log.info("Requesting Company Overview from Alpha Vantage for symbol: {}", symbol);

       Map<String, Object> rawResponse = restTemplate.getForObject(uri, Map.class);

       if (rawResponse == null || rawResponse.isEmpty()) {

           return null;
       }

       return objectMapper.convertValue(rawResponse, CompanyOverviewDto.class);
   }
}
